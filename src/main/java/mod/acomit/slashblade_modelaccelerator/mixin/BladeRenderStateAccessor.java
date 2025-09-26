package mod.acomit.slashblade_modelaccelerator.mixin;

import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.awt.*;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-09-26 13:25
 * @Description: TODO
 */
@Mixin(BladeRenderState.class)
public interface BladeRenderStateAccessor {
    @Accessor(value = "col", remap = false)
    static Color getCol() {
        throw new AssertionError();
    }

}
