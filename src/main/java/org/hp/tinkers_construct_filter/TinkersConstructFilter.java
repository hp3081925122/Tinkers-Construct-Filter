package org.hp.tinkers_construct_filter;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.hp.tinkers_construct_filter.client.config.ClientConfig;
import org.slf4j.Logger;

@Mod(TinkersConstructFilter.MOD_ID)
public final class TinkersConstructFilter {
    public static final String MOD_ID = "tinkers_construct_filter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TinkersConstructFilter() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
