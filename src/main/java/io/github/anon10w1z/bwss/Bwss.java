package io.github.anon10w1z.bwss;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = BwssConstants.MODID, name = BwssConstants.NAME, version = BwssConstants.VERSION, updateJSON = BwssConstants.UPDATE_JSON)
public class Bwss {
    @Mod.Instance
    private static Bwss instance;

    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(instance);
    }

    public static Bwss getInstance() {
        return instance;
    }
}
