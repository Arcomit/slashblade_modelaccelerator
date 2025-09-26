package mod.acomit.slashblade_modelaccelerator.obj.render;

import mod.arcomit.anran.core.obj.utils.IrisUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.glMapNamedBufferRange;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-09-25 10:19
 * @Description: TODO
 */
public class UVDynamicUpdater {
    private int uvVBO = 0;
    private long syncObject = 0; // 添加同步对象
    private FloatBuffer persistentBuffer = null;
    private FloatBuffer currentBuffer = null;
    private FloatBuffer pendingBuffer = null;
    private int maxVertices;
    private boolean isInitialized = false;

    public void initUVBuffer(int maxVertices) {
        if (isInitialized) {
            throw new IllegalStateException("UV buffer already initialized");
        }
        if (maxVertices <= 0) {
            throw new IllegalArgumentException("maxVertices must be positive");
        }

        this.maxVertices = maxVertices;

        uvVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, uvVBO);

        long bufferSize = (long) maxVertices * 2 * Float.BYTES;
        glBufferStorage(GL_ARRAY_BUFFER, bufferSize,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);

        persistentBuffer = glMapNamedBufferRange(
                uvVBO,
                0,
                bufferSize,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT
        ).asFloatBuffer();

        // 创建独立位置的视图
        currentBuffer = persistentBuffer.duplicate();
        pendingBuffer = persistentBuffer.duplicate();

        // 初始化为空缓冲区
        pendingBuffer.limit(0);

        glEnableVertexAttribArray(IrisUtils.vaUV0);
        glVertexAttribPointer(
                IrisUtils.vaUV0, // 属性位置
                2,                      // 每个顶点的分量数 (RGBA)
                GL_FLOAT,               // 数据类型
                false,                  // 是否归一化
                0,                      // 步长 (紧密排列)
                0                       // 偏移量
        );

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        isInitialized = true;
    }

    // 统一更新接口
    public void updateUVs(FloatBuffer uvBuffer, int startVertex, int count) {
        if (!isInitialized) {
            throw new IllegalStateException("UV buffer not initialized");
        }

        final int startPos = startVertex * 2;
        final int totalElements = count * 2;

        if (startVertex < 0 || count < 0 || // 允许count=0
                (startVertex + count) > maxVertices ||
                (uvBuffer != null && uvBuffer.remaining() < totalElements)) {
            throw new IllegalArgumentException("Invalid update parameters");
        }

        // 等待可能未完成的GPU操作
        waitForGpu();

        // 定位并写入数据
        pendingBuffer.limit(persistentBuffer.capacity());
        pendingBuffer.position(startPos);

        if (uvBuffer != null) {
            pendingBuffer.put(uvBuffer);
        } else {
            // 可选：清除区域
            for (int i = 0; i < totalElements; i++) {
                pendingBuffer.put(0.0f);
            }
        }
        pendingBuffer.flip();
    }

    // 渲染前同步
    public void beforeRender() {
        if (!isInitialized) return;

        // 创建新的同步对象并等待旧对象
        long prevSync = syncObject;
        syncObject = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

        if (prevSync != 0) {
            glWaitSync(prevSync, 0, GL_TIMEOUT_IGNORED);
            glDeleteSync(prevSync);
        }

        // 交换缓冲区
        flipBuffers();
    }

    private void waitForGpu() {
        if (syncObject != 0) {
            while (glClientWaitSync(syncObject, GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000_000) == GL_TIMEOUT_EXPIRED) {
                // 重试或记录警告
            }
            glDeleteSync(syncObject);
            syncObject = 0;
        }
    }

    private void flipBuffers() {
        FloatBuffer temp = currentBuffer;
        currentBuffer = pendingBuffer;
        pendingBuffer = temp;
    }

    public void cleanup() {
        if (!isInitialized) return;

        waitForGpu(); // 确保GPU完成操作

        if (syncObject != 0) {
            glDeleteSync(syncObject);
            syncObject = 0;
        }

        if (uvVBO != 0) {
            glBindBuffer(GL_ARRAY_BUFFER, uvVBO);
            glUnmapBuffer(GL_ARRAY_BUFFER);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDeleteBuffers(uvVBO);
            uvVBO = 0;
        }

        persistentBuffer = null;
        currentBuffer = null;
        pendingBuffer = null;
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
