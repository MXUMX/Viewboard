package com.mx.viewboard;

import com.mx.viewboard.client.ViewBoardClientEvents;
import net.fabricmc.api.ClientModInitializer;

public final class ViewBoardMod implements ClientModInitializer {
    public static final String MOD_ID = "viewboard";

    @Override
    public void onInitializeClient() {
        ViewBoardClientEvents.register();
    }
}
