package mod.acomit.slashblade_modelaccelerator.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.acomit.slashblade_modelaccelerator.ModelAccelerator;
import mod.acomit.slashblade_modelaccelerator.mixin.BladeRenderStateAccessor;
import mod.acomit.slashblade_modelaccelerator.obj.ObjModel;
import mod.acomit.slashblade_modelaccelerator.obj.event.ObjModelManager;
import mod.acomit.slashblade_modelaccelerator.utils.DefaultResources;
import mod.acomit.slashblade_modelaccelerator.utils.WriteVerticesInfo;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.function.Function;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-09-25 17:57
 * @Description: TODO
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = ModelAccelerator.MODID, value = Dist.CLIENT)
public class AccelerateHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRender(RenderOverrideEvent event) {
        ItemStack blade = event.getStack();
        String target = event.getTarget();
        if (blade.getItem() instanceof ItemSlashBlade) {
            event.setCanceled(true);

            ResourceLocation texture = event.getTexture();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource buffer = event.getBuffer();
            int packedLightIn = event.getPackedLightIn();
            Function<ResourceLocation, RenderType> getRenderType = event.getGetRenderType();
            boolean enableEffect = event.isEnableEffect();

            ObjModel model;
            WavefrontObject durabilityModel = BladeModelManager.getInstance()
                    .getModel(mods.flammpfeil.slashblade.init.DefaultResources.resourceDurabilityModel);
            if (event.getModel().equals(durabilityModel)){
                model = ObjModelManager.get(mods.flammpfeil.slashblade.init.DefaultResources.resourceDurabilityModel);
                poseStack.scale(1.01f, 1.01f, 1.01f);
            }else {
                ResourceLocation modelLocation = blade.getCapability(ItemSlashBlade.BLADESTATE)
                        .filter(s -> s.getModel().isPresent()).map(s -> s.getModel().get())
                        .orElse(DefaultResources.DEFAULT_MODEL);

                model = ObjModelManager.get(modelLocation);
            }

            RenderType rt = getRenderType.apply(texture);
            VertexConsumer vc = buffer.getBuffer(rt);//这行不要去掉，将 renderType 放入 MultiBufferSource 中，确保渲染将在 Iris 批量实体渲染中运行。

            WriteVerticesInfo.setLightMap(packedLightIn);
            WriteVerticesInfo.setPoseStack(poseStack);

            Color currentColor = null;
            try {
                currentColor = BladeRenderStateAccessor.getCol();
            } catch (AssertionError e) {
                currentColor = Color.white;
            }
            if (currentColor == null) {
                currentColor = Color.white;
            }
            WriteVerticesInfo.setColor(currentColor);
            WriteVerticesInfo.setAlphaOverride(Face.alphaOverride);
            WriteVerticesInfo.setUvOperator(Face.uvOperator);

            model.renderOnly(rt, target);
            if (blade.hasFoil() && enableEffect) {
                rt = BladeRenderState.getSlashBladeGlint();
                vc = buffer.getBuffer(rt);
                model.renderOnly(rt, target);
            }
            WriteVerticesInfo.resetLightMap();
            WriteVerticesInfo.resetPoseStack();
            WriteVerticesInfo.resetColor();
            WriteVerticesInfo.resetAlphaOverride();
            WriteVerticesInfo.resetUvOperator();

            // WTF，我们在玩什么套娃游戏
            Face.resetMatrix();
            Face.resetLightMap();
            Face.resetCol();
            Face.resetAlphaOverride();
            Face.resetUvOperator();
            BladeRenderState.resetCol();
        }
    }
}
