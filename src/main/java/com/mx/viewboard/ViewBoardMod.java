package com.mx.viewboard;

import com.mx.viewboard.client.ViewBoardClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(ViewBoardMod.MOD_ID)
public final class ViewBoardMod {
    public static final String MOD_ID = "viewboard";

    public ViewBoardMod() {
        MinecraftForge.EVENT_BUS.register(ViewBoardClientEvents.class);
    }
}
