package princ.animatedloadingoverlay.client.mixin.pack.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import princ.animatedloadingoverlay.client.duck.pack.resources.SimpleReloadInstanceDuck;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SimpleReloadInstance.class)
public class SimpleReloadInstanceMixin implements SimpleReloadInstanceDuck {

    @Unique
    private PreparationBarrier animatedLoadingOverlay$initialTask;

    @Unique
    private boolean animatedLoadingOverlay$animationFinished;

    @Override
    public boolean animatedLoadingOverlay$animationFinished() {
        return this.animatedLoadingOverlay$animationFinished;
    }

    @Override
    public void animatedLoadingOverlay$animationFinished(boolean finished) {
        this.animatedLoadingOverlay$animationFinished = finished;

        if (finished) {
            this.animatedLoadingOverlay$initialTask.complete(Unit.INSTANCE);
        }
    }

    @WrapOperation(
            method = "startTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/SimpleReloadInstance;prepareTasks(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Lnet/minecraft/server/packs/resources/SimpleReloadInstance$StateFactory;Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    <S> CompletableFuture<List<S>> animatedLoadingOverlay$delayPreparation(SimpleReloadInstance<S> reloadInstance, Executor taskExecutor, Executor mainThreadExecutor, ResourceManager resourceManager, List<PreparableReloadListener> listeners, @Coerce Object stateFactory, CompletableFuture<?> initialTask, Operation<CompletableFuture<List<S>>> original) {
        if (initialTask instanceof PreparationBarrier) {
            this.animatedLoadingOverlay$initialTask = (PreparationBarrier) initialTask;
            return initialTask.thenCompose(ignored -> original.call(reloadInstance, taskExecutor, mainThreadExecutor, resourceManager, listeners, stateFactory, initialTask));
        }
        return original.call(reloadInstance, taskExecutor, mainThreadExecutor, resourceManager, listeners, stateFactory, initialTask);
    }
}