package mod.acomit.slashblade_modelaccelerator.obj;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import mod.acomit.slashblade_modelaccelerator.render.ColorDynamicUpdater;
import mod.acomit.slashblade_modelaccelerator.render.ModelRenderer;
import mod.acomit.slashblade_modelaccelerator.render.UVDynamicUpdater;
import mod.acomit.slashblade_modelaccelerator.utils.IrisUtils;
import mod.acomit.slashblade_modelaccelerator.utils.RenderUtils;
import mod.acomit.slashblade_modelaccelerator.utils.WriteVerticesInfo;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.BiFunction;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-09-25 10:27
 * @Description: TODO
 */
public class RenderCache {

    protected int VAO;
    protected int VBO;
    protected int EBO;
    protected int indexCount;
    protected ColorDynamicUpdater colorDynamicUpdater = new ColorDynamicUpdater();
    private FloatBuffer reusableColorBuffer;
    protected UVDynamicUpdater uvDynamicUpdater = new UVDynamicUpdater();
    private FloatBuffer reusableUVBuffer;
    protected boolean initialized = false;

    private final ObjGroup group;

    public RenderCache(ObjGroup group) {
        this.group = group;
    }

    public void init(){
        BufferBuilder bufferBuilder = new BufferBuilder(256);
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
        // 向bufferBuilder输入顶点数据
        WriteVerticesInfo.resetPoseStack();
        group.writeVertices(bufferBuilder);

        BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();
        BufferBuilder.DrawState drawState = renderedBuffer.drawState();
        ByteBuffer vertexBuffer = renderedBuffer.vertexBuffer();

        VAO = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(VAO);

        VBO = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, VBO);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertexBuffer, GL30.GL_STATIC_DRAW);
        drawState.format().setupBufferState();

        int vertexCount = group.getVertexCount();
        colorDynamicUpdater.initColorBuffer(vertexCount);
        reusableColorBuffer = ByteBuffer.allocateDirect(vertexCount * 4 * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        uvDynamicUpdater.initUVBuffer(vertexCount);
        reusableUVBuffer = ByteBuffer.allocateDirect(vertexCount * 2 * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        indexCount = drawState.indexCount();
        int newCapacity = Math.max(indexCount * 2, 65536);
        IntBuffer intBuffer = ByteBuffer.allocateDirect(newCapacity * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();

        for (int i = 0; i < indexCount; i++) {
            intBuffer.put(i, i);
        }

        intBuffer.position(0);
        intBuffer.limit(indexCount);
        EBO = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, EBO);
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, intBuffer, GL30.GL_STATIC_DRAW);

        //初始化结束
        GL30.glBindVertexArray(0);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0);
        initialized = true;
    }

    public void render(RenderStateShard renderType) {
        if (!initialized) {
            init();
        }
        PoseStack poseStack = RenderUtils.copyPoseStack(WriteVerticesInfo.getPoseStack());
        int overlay = WriteVerticesInfo.getOverlayMap();
        int light = WriteVerticesInfo.getLightMap();
        Color color = WriteVerticesInfo.getColor();
        BiFunction<Vector4f, Integer, Integer> alphaOverride = WriteVerticesInfo.getAlphaOverride();
        Vector4f nowUvOperator = WriteVerticesInfo.getUvOperator();
        ModelRenderer.addRenderCommand(renderType, () -> {
            if (poseStack == null) return;
            ShaderInstance shader = RenderSystem.getShader();
            int currentProgram = shader.getId();
            if (shader.MODEL_VIEW_MATRIX != null) {
                Matrix4f currentModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
                Matrix4f poseMatrix = new Matrix4f(poseStack.last().pose());
                currentModelView.mul(poseMatrix);
                shader.MODEL_VIEW_MATRIX.set(currentModelView);
            }
            int nm = GL20.glGetUniformLocation(currentProgram, "iris_NormalMat");
            if (nm >= 0) {
                Matrix3f normalMatrix = new Matrix3f(poseStack.last().normal());
                FloatBuffer buffer = BufferUtils.createFloatBuffer(9);
                normalMatrix.get(buffer);
                GL20.glUniformMatrix3fv(nm, false, buffer);
            }
            shader.apply();
            GL30.glBindVertexArray(VAO);
            // 颜色
            reusableColorBuffer.clear();
            if (!group.getFaces().isEmpty()) {
                for (ObjFace face : group.getFaces()) {
                    face.calculateColor(reusableColorBuffer, color, alphaOverride);
                }
            }
            reusableColorBuffer.flip();
            colorDynamicUpdater.updateColors(reusableColorBuffer, 0, reusableColorBuffer.remaining()/4);
            colorDynamicUpdater.beforeRender();
            // uv
            reusableUVBuffer.clear();
            if (!group.getFaces().isEmpty()) {
                for (ObjFace face : group.getFaces()) {
                    face.calculateUV(reusableUVBuffer, nowUvOperator, 0.0005F);
                }
            }
            reusableUVBuffer.flip();
            uvDynamicUpdater.updateUVs(reusableUVBuffer, 0, reusableUVBuffer.remaining()/2);
            uvDynamicUpdater.beforeRender();
            // 光照和覆盖层
            GL30.glDisableVertexAttribArray(IrisUtils.vaUV1);
            GL30.glVertexAttribI2i(IrisUtils.vaUV1, overlay & '\uffff', overlay >> 16 & '\uffff');
            GL30.glDisableVertexAttribArray(IrisUtils.vaUV2);
            GL30.glVertexAttribI2i(IrisUtils.vaUV2, light & '\uffff', light >> 16 & '\uffff');

            //绘制
            GL30.glDrawElements(
                    GL30.GL_TRIANGLES,
                    indexCount,
                    GL30.GL_UNSIGNED_INT,
                    0
            );

            GL30.glBindVertexArray(0);
            shader.clear();
        });
    }
}
