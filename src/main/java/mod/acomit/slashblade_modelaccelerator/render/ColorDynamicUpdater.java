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
 * @CreateTime: 2025-09-25 10:17
 * @Description: TODO
 */
public class ColorDynamicUpdater {
    private int colorVBO = 0;
    private long syncObject = 0;

    // 缓冲区管理
    private FloatBuffer persistentBuffer = null;
    private FloatBuffer currentBuffer = null;
    private FloatBuffer pendingBuffer = null;

    // 配置参数
    private int maxVertices;
    private boolean isInitialized = false;

    public void initColorBuffer(int maxVertices) {
        if (isInitialized) {
            throw new IllegalStateException("Color buffer already initialized");
        }
        if (maxVertices <= 0) {
            throw new IllegalArgumentException("maxVertices must be positive");
        }

        this.maxVertices = maxVertices;

        // 创建并配置VBO
        colorVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, colorVBO);

        // 计算缓冲区大小 (每个顶点4个float: RGBA)
        long bufferSize = (long) maxVertices * 4 * Float.BYTES;
        glBufferStorage(GL_ARRAY_BUFFER, bufferSize,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);

        // 映射持久化缓冲区
        persistentBuffer = glMapNamedBufferRange(
                colorVBO,
                0,
                bufferSize,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT
        ).asFloatBuffer();

        // 创建缓冲区视图
        currentBuffer = persistentBuffer.duplicate();
        pendingBuffer = persistentBuffer.duplicate();
        pendingBuffer.limit(0); // 初始化为空

        // 配置顶点属性
        glEnableVertexAttribArray(IrisUtils.vaColor);
        glVertexAttribPointer(
                IrisUtils.vaColor, // 属性位置
                4,                      // 每个顶点的分量数 (RGBA)
                GL_FLOAT,               // 数据类型
                false,                  // 是否归一化
                0,                      // 步长 (紧密排列)
                0                       // 偏移量
        );

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        isInitialized = true;
    }

    public void updateColors(FloatBuffer colorBuffer, int startVertex, int count) {
        if (!isInitialized) {
            throw new IllegalStateException("Color buffer not initialized");
        }

        final int startPos = startVertex * 4; // 每个顶点4个分量
        final int totalElements = count * 4;

        // 参数验证
        if (startVertex < 0 || count < 0 ||
                (startVertex + count) > maxVertices ||
                (colorBuffer != null && colorBuffer.remaining() < totalElements)) {
            throw new IllegalArgumentException("Invalid update parameters");
        }

        waitForGpu(); // 等待GPU完成操作

        // 准备写入位置
        pendingBuffer.limit(persistentBuffer.capacity());
        pendingBuffer.position(startPos);

        if (colorBuffer != null) {
            // 写入颜色数据
            for (int i = 0; i < totalElements; i++) {
                pendingBuffer.put(colorBuffer.get());
            }
        } else {
            // 清除区域 (设为白色不透明)
            for (int i = 0; i < totalElements; i++) {
                pendingBuffer.put(1.0f);
            }
        }
        pendingBuffer.flip();
    }

    public void beforeRender() {
        if (!isInitialized) return;

        // 管理同步对象
        long prevSync = syncObject;
        syncObject = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

        if (prevSync != 0) {
            glWaitSync(prevSync, 0, GL_TIMEOUT_IGNORED);
            glDeleteSync(prevSync);
        }

        flipBuffers(); // 交换缓冲区
    }

    private void waitForGpu() {
        if (syncObject != 0) {
            int result;
            do {
                result = glClientWaitSync(syncObject, GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000_000);
            } while (result == GL_TIMEOUT_EXPIRED);

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

        waitForGpu();

        // 清理同步对象
        if (syncObject != 0) {
            glDeleteSync(syncObject);
            syncObject = 0;
        }

        // 清理VBO
        if (colorVBO != 0) {
            glBindBuffer(GL_ARRAY_BUFFER, colorVBO);
            glUnmapBuffer(GL_ARRAY_BUFFER);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDeleteBuffers(colorVBO);
            colorVBO = 0;
        }

        // 清理引用
        persistentBuffer = null;
        currentBuffer = null;
        pendingBuffer = null;
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
