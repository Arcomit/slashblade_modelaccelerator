package mod.arcomit.anran.core.obj;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-08-05 12:10
 * @Description: 模型组
 */
@OnlyIn(Dist.CLIENT)
@Getter@Setter
public class ObjGroup {

    private final String        name;
    private final List<ObjFace> faces    = new ArrayList<>();
    private       int           vertexCount;

    private final RenderCache renderCache;

    public ObjGroup(String name) {
        this.name = name;
        this.renderCache = new RenderCache(this);
    }

    public void writeVertices(VertexConsumer vertexConsumer){
        for (ObjFace face : faces) {

            face.writeVertices(vertexConsumer);

        }
    }

    public void init(){
        renderCache.init();
    }

    public void render(RenderStateShard renderType){
        renderCache.render(renderType);
    }
}
