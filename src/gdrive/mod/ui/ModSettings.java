package gdrive.mod.ui;

import gdrive.mod.GDriveControl.GDriveController;
import arc.Core;
import arc.files.Fi;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;

import java.io.IOException;
import java.lang.reflect.Field;

import static mindustry.Vars.*;

public class ModSettings {
    private static final String BASE_DATA_FILE_NAME = "/java.org.mod.SyncDataModRest-MindustryData";
    private static final String DATA_DIRECTORY = Core.settings.getDataDirectory().path();

    static BaseDialog gdriveDialog = new BaseDialog("@gdrive-dialog");
    static SettingsMenuDialog smd = new SettingsMenuDialog();
    static GDriveController gDriveController = new GDriveController();

    public static void init() {
        gdriveDialog.addCloseButton();
        gdriveDialog.cont.table(Tex.button, t -> {
            t.defaults().size(280f, 60f).left();

            t.row();
            t.button("@btn-export", Icon.upload, Styles.flatt, ModSettings::exportDataToGD);

            t.row();
            t.button("@btn-import", Icon.download, Styles.flatt, () -> ui.showConfirm("@confirm", "@data.import.confirm", ModSettings::importDataFromGD));
        });

        Table dataDialog;
        try {
            dataDialog = getDataDialog();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        dataDialog.row();
        dataDialog.button("@btn-gdrive", Icon.settings, Styles.cleart, () -> gdriveDialog.show());
    }

    public static void exportDataToGD() {
        Log.info("Start export data to Google Drive");

        Fi dataFile = Fi.get(DATA_DIRECTORY + BASE_DATA_FILE_NAME + ".zip");
        try {
            smd.exportData(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Log.info("temp data file created " + dataFile.absolutePath());
        gDriveController.uploadFile(dataFile);
    }

    public static void importDataFromGD() {
        Log.info("Start import data from Google Drive");
        Fi dataFile = Fi.get(DATA_DIRECTORY + BASE_DATA_FILE_NAME + ".zip");
        gDriveController.downloadFile(dataFile);
    }

    public static Table getDataDialog() throws NoSuchFieldException, IllegalAccessException {
        Field field = ui.settings.getClass().getDeclaredField("dataDialog");
        field.setAccessible(true);
        BaseDialog dataDialog = (BaseDialog) field.get(ui.settings);
        return (Table) ((Group) (dataDialog.getChildren().get(1))).getChildren().get(0);
    }
}