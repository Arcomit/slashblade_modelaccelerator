package mod.acomit.slashblade_modelaccelerator.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-09-24 11:38
 * @Description: TODO
 */
public class ModelRenderer {
    // 当前的渲染状态
    private static RenderStateShard currentRenderStateShard;

    public static void setCurrentRenderStateShard(RenderStateShard renderStateShard) {
        if (ModList.get().isLoaded(Iris.MODID)) {
            currentRenderStateShard = ((renderStateShard instanceof WrappableRenderType) ?
                    ((WrappableRenderType)renderStateShard).unwrap() : renderStateShard);
        }else {
            currentRenderStateShard = renderStateShard;
        }
    }

    private static final Map<RenderStateShard, List<Runnable>> RENDER_COMMANDS = new LinkedHashMap<>();

    public static void addRenderCommand(RenderStateShard renderStateShard, Runnable command) {
        if (RENDER_COMMANDS.containsKey(renderStateShard)){
            RENDER_COMMANDS.get(renderStateShard).add(command);
        }else{
            List<Runnable> renderCommands = new ArrayList<>();
            renderCommands.add(command);
            RENDER_COMMANDS.put(renderStateShard, renderCommands);
        }
    }

    public static void runRenderCommands() {
        if (currentRenderStateShard != null) {
            List<Runnable> renderCommands = RENDER_COMMANDS.remove(currentRenderStateShard);
            if (renderCommands != null) {
                int currentVAO = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
                int currentArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
                int currentElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
                ShaderInstance shader = RenderSystem.getShader();
                for (int i = 0; i < 12; ++i) {
                    int j = RenderSystem.getShaderTexture(i);
                    shader.setSampler("Sampler" + i, j);
                }
                if (shader.PROJECTION_MATRIX != null) {
                    shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
                }
                if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
                    shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
                }
                if (shader.COLOR_MODULATOR != null) {
                    shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
                }
                if (shader.FOG_START != null) {
                    shader.FOG_START.set(RenderSystem.getShaderFogStart());
                }
                if (shader.FOG_END != null) {
                    shader.FOG_END.set(RenderSystem.getShaderFogEnd());
                }
                if (shader.FOG_COLOR != null) {
                    shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
                }
                if (shader.FOG_SHAPE != null) {
                    shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
                }
                if (shader.TEXTURE_MATRIX != null) {
                    shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
                }
                if (shader.GAME_TIME != null) {
                    shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
                }
                if (shader.SCREEN_SIZE != null) {
                    Window window = Minecraft.getInstance().getWindow();
                    shader.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
                }
                RenderSystem.setupShaderLights(shader);
                shader.apply();

                renderCommands.forEach(Runnable::run);

                shader.clear();
                GL30.glBindVertexArray(currentVAO);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, currentArrayBuffer);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
            }
        }
    }
}
