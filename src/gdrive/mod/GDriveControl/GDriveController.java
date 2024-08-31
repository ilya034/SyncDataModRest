package gdrive.mod.GDriveControl;

import gdrive.mod.OAuthControl.OAuthController;
import arc.files.Fi;
import arc.util.Log;

public class GDriveController {
    OAuthController authController;

    public GDriveController() {
    }

    public OAuthController getAuthController() {
        if (authController == null) {
            Log.info("Return new OAuthController");
            return new OAuthController();
        } else {
            return authController;
        }
    }

    public void downloadFile(Fi dataFile) {
        authController = getAuthController();
    }

    public void uploadFile(Fi dataFile) {
        authController = getAuthController();
    }
}
