package mod.acomit.slashblade_modelaccelerator.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix3f;
import org.joml.Vector3f;
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

                renderCommands.forEach(Runnable::run);

                GL30.glBindVertexArray(currentVAO);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, currentArrayBuffer);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
            }
        }
    }
}
