package mod.acomit.slashblade_modelaccelerator.obj.event;

import mod.acomit.slashblade_modelaccelerator.ModelAccelerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-08-05 11:54
 * @Description: 用于在启动游戏，重载材质包时预加载模型
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = ModelAccelerator.MODID)
public class PreloadedModelHandler {

    @SubscribeEvent
    public static void onPreload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ObjModelManager());
    }
}
