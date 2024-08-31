package gdrive.mod;

import arc.*;
import mindustry.game.EventType;
import mindustry.mod.*;
import gdrive.mod.ui.ModSettings;

public class SyncDataModRest extends Mod {
    public SyncDataModRest() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            ModSettings.init();
        });
    }
}