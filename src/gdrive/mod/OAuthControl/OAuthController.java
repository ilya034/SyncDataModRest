package gdrive.mod.OAuthControl;

import arc.util.Log;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import static mindustry.Vars.*;

public class OAuthController {
    public static final String SCOPE = "https://www.googleapis.com/auth/drive.file";

    // private final String state = getRandomString();
    private final String codeVerifier = getRandomString();
    private final String codeChallenge = getCodeChallengeString();

    private HttpServer httpServer;
    private String clientId;
    private String clientSecret;
    private String authEndpoint;
    private String exTokensEndpoint;
    private String redirectUri;

    public OAuthController() {
        loadCredentials();
        createHttpServer();
        updateRedirectUri();
        startAuth();
    }

    public void loadCredentials() {
        JSONObject secretsJson = (JSONObject) loadCredentialsJson().get("installed");

        clientId = (String) secretsJson.get("client_id");
        clientSecret = (String) secretsJson.get("client_secret");
        authEndpoint = (String) secretsJson.get("auth_uri");
        exTokensEndpoint = (String) secretsJson.get("token_uri");
        redirectUri = (String) ((JSONArray) secretsJson.get("redirect_uris")).get(0);
    }

    public JSONObject loadCredentialsJson() {
        String secretFileName = android ? "client_secret_android.json" : "client_secret_desktop.json";
        try {
            InputStream in = OAuthController.class.getClassLoader().getResourceAsStream("gdrive/" + secretFileName);
            return (JSONObject) new JSONParser().parse(new Scanner(in).useDelimiter("\\A").next());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void startAuth() {
        try {
            Desktop.getDesktop().browse(createAuthCodeRequestURI());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exchangeAuthCodeForTokens(String authCode) throws IOException {
        HttpURLConnection exchangeTokenResponse = requestAuthCodeExchange(createAuthCodeExchangeURI(authCode));

        if (exchangeTokenResponse.getResponseCode() != 200) {
            Log.err("Failed to authorize: exchangeAuthCodeResponse.statusCode not OK");
            ui.showException(new Throwable("Failed to authorize: exchangeAuthCodeResponse.statusCode not OK"));
            return;
        } else {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(exchangeTokenResponse.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            parseTokens(response.toString());
        }
    }

    public URI createAuthCodeRequestURI() {
        return URI.create(authEndpoint +
                "?response_type=code" +
                "&scope=" + SCOPE +
                "&redirect_uri=" + redirectUri +
                "&client_id=" + clientId +
                // "&state=" + state.replace("=", "%3D") +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256");
    }

    public URI createAuthCodeExchangeURI(String code) {
        return URI.create(exTokensEndpoint +
                "?code=" + code +
                "&redirect_uri=" + redirectUri +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code_verifier=" + codeVerifier +
                "&grant_type=authorization_code");
    }

    public HttpURLConnection requestAuthCodeExchange(URI requestUri) {
        try {
            Log.info(requestUri.toString());
            HttpURLConnection connection = (HttpURLConnection) (new URL(requestUri.toString())).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().close();
            return connection;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseTokens(String responseBody) {
        try {
            JSONObject json = (JSONObject) new JSONParser().parse(responseBody);
            Log.info("Access token: " + json.get("access_token") + ", expires in: " + json.get("expires_in"));
            Log.info("Refresh token: " + json.get("refresh_token"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void createHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            httpServer.createContext("/", new BaseHandler());
            httpServer.setExecutor(null);
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateRedirectUri() {
        redirectUri = redirectUri + ":" + httpServer.getAddress().getPort();
    }

    public static String getRandomString() {
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[32];
        random.nextBytes(r);
        return Base64.getUrlEncoder().encodeToString(r);
    }

    public String getCodeChallengeString() {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(MessageDigest
                            .getInstance("SHA-256")
                            .digest(codeVerifier.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public class BaseHandler implements HttpHandler {
        public String getParamFromQuery(String query, String param) {
            String[] params = query.split("&");

            for (String par : params) {
                if (par.substring(0, par.indexOf("=")).contains(param)) {
                    return par.substring(par.indexOf("=") + 1);
                }
            }
            return null;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String q = exchange.getRequestURI().getQuery();
            if (q.contains("code")) {
                String authCode = getParamFromQuery(q, "code");
                exchangeAuthCodeForTokens(authCode);
            }

            String response = "You may close this window";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            httpServer.stop(5);
        }
    }
}
