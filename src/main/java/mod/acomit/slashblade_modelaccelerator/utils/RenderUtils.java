package mod.acomit.slashblade_modelaccelerator.utils;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-09-25 10:40
 * @Description: TODO
 */
public class RenderUtils {
    public static PoseStack copyPoseStack(PoseStack poseStack) {
        if (poseStack == null) return null;
        PoseStack finalStack = new PoseStack();
        finalStack.setIdentity();
        finalStack.poseStack.addLast(poseStack.last());
        return finalStack;
    }
}
