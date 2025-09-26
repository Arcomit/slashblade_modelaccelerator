package mod.acomit.slashblade_modelaccelerator;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@SuppressWarnings("removal")
@Mod(ModelAccelerator.MODID)
public class ModelAccelerator {

    public static final String MODID = "slashblade_modelaccelerator";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModelAccelerator() {
        LOGGER.info(MODID + " is loaded!");
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        IEventBus modEventBus = context.getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModelAccelerator.MODID, path);
    }
}
