package com.rickg.angelicascale;

import com.rickg.angelicascale.client.RenderHookState;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderHookState.init();
    }
}
