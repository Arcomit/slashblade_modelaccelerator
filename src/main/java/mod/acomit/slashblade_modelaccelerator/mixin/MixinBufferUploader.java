package mod.acomit.slashblade_modelaccelerator.obj.render.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import mod.arcomit.anran.core.obj.render.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferUploader.class)
public class MixinBufferUploader {

    @Inject(method = "_drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V", at = @At("HEAD"))
    private static void before_drawWithShader(BufferBuilder.RenderedBuffer renderedBuffer, CallbackInfo ci) {
        ModelRenderer.runRenderCommands();
    }

}