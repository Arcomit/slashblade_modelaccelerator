package mod.acomit.slashblade_modelaccelerator.obj.event;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import mod.acomit.slashblade_modelaccelerator.ModelAccelerator;
import mod.acomit.slashblade_modelaccelerator.obj.ModelParseException;
import mod.acomit.slashblade_modelaccelerator.obj.ObjModel;
import mod.acomit.slashblade_modelaccelerator.obj.ObjReader;
import mod.acomit.slashblade_modelaccelerator.utils.DefaultResources;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-08-05 11:54
 * @Description: Obj模型管理，用于加载和缓存，获取Obj模型。
 */
@OnlyIn(Dist.CLIENT)
public class ObjModelManager implements PreparableReloadListener {

    private static final    ResourceLocation                MODEL_DIR     = SlashBlade.prefix("model");
    private static final    String                          FILE_TYPES    = ".obj";

    private static final    Lock                            LOCK          = new ReentrantLock();
    private static final    Condition                       PREPARED      = LOCK.newCondition();
    private static volatile Map<ResourceLocation, ObjModel> modelsCache;

    @Override
    public @NotNull CompletableFuture<Void> reload(
                     PreparationBarrier preparationBarrier,
            @NotNull ResourceManager    resourceManager,
            @NotNull ProfilerFiller     preparationsProfiler,
            @NotNull ProfilerFiller     reloadProfiler,
            @NotNull Executor           backgroundExecutor,
            @NotNull Executor           gameExecutor
    ) {
        return CompletableFuture
                .runAsync(
                        () -> {
                            loadResources(resourceManager);
                        }
                        , backgroundExecutor
                )
                .thenCompose(preparationBarrier::wait);
    }

    private void loadResources(ResourceManager resourceManager) {
        LOCK.lock();
        modelsCache = null;
        LOCK.unlock();

        Map<ResourceLocation, ObjModel> cache = Maps.newHashMap();

        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                MODEL_DIR.getPath(),
                resLoc -> resLoc.getNamespace().equals(MODEL_DIR.getNamespace())
                        && resLoc.getPath().toLowerCase(Locale.ROOT).endsWith(FILE_TYPES)
        );

        resources.forEach((resourceLocation, resource) -> {
            ObjModel model;

            try {

                model = new ObjReader(resource).getModel();
                if (!RenderSystem.isOnRenderThread()) {
                    RenderSystem.recordRenderCall(model::initAll);
                }else {
                        model.initAll();
                }

            } catch (IOException | ModelParseException e) {

                throw new RuntimeException("Failed to load model: " + resourceLocation, e);

            }

            cache.put(resourceLocation, model);
        });

        // 通知所有等待线程 modelsCache 已准备
        LOCK.lock();
        try {
            modelsCache = cache;
            PREPARED.signalAll();
        } finally {
            LOCK.unlock();
        }
    }

    public static ObjModel get(ResourceLocation resourceLocation) {
        LOCK.lock();
        try {
            while (modelsCache == null) {
                PREPARED.await(); // 等待 modelsCache 准备好
            }
            return modelsCache.computeIfAbsent(resourceLocation,
                    resLocation -> {
                        try {

                            return new ObjReader(resourceLocation).getModel();

                        } catch (IOException | ModelParseException e) {

                            ModelAccelerator.LOGGER.warn("Failed to load model: {}", resourceLocation, e);

                            return ObjModelManager.get(DefaultResources.DEFAULT_MODEL);

                        }
                    }
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            LOCK.unlock();
        }
    }
}
