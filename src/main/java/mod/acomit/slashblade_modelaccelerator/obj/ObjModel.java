package mod.acomit.slashblade_modelaccelerator.obj;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-08-05 12:08
 * @Description: 模型主类
 */
@OnlyIn(Dist.CLIENT)
@Getter
public class ObjModel {

    private final Map<String, ObjGroup> Groups = new HashMap<>();

    /**
     * 写入所有模型组的顶点
     * @param vertexConsumer 需要写入的顶点消费者
     */
    public void writeVerticesAll(VertexConsumer vertexConsumer){
        for (ObjGroup group : Groups.values()) {

            group.writeVertices(vertexConsumer);

        }
    }

    /**
     * 写入指定模型组的顶点
     * @param vertexConsumer 需要写入的顶点消费者
     * @param groupName      指定的模型组名
     */
    public void writeVerticesOnly(VertexConsumer vertexConsumer, String... groupName){
        for (String name : groupName) {

            writeVerticesOnly(vertexConsumer, name);

        }
    }

    /**
     * 写入指定模型组的顶点(单个组)
     * @param vertexConsumer 需要写入的顶点消费者
     * @param groupName      指定的模型组名
     */
    public void writeVerticesOnly(VertexConsumer vertexConsumer, String groupName){
        ObjGroup group = Groups.get(groupName);
        if (group != null) {

            group.writeVertices(vertexConsumer);

        }
    }


    //_____________________________________________________________________________________


    @OnlyIn(Dist.CLIENT)
    public void initAll(){
        for (ObjGroup groupObject : Groups.values()) {
            if (groupObject.getVertexCount() != 0){
                groupObject.init();
            }
        }
    }

    public void renderAll(RenderStateShard renderType){
        for (ObjGroup group : Groups.values()) {

            group.render(renderType);

        }
    }

    public void renderOnly(RenderStateShard renderType, String... groupName){
        for (String name : groupName) {

            renderOnly(renderType, name);

        }
    }

    public void renderOnly(RenderStateShard renderType, String groupName){
        ObjGroup group = Groups.get(groupName);
        if (group != null) {

            group.render(renderType);

        }
    }
}
