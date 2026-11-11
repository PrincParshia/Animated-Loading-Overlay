package princ.animatedloadingoverlay.client.mixin.pack.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import princ.animatedloadingoverlay.client.duck.pack.resources.SimpleReloadInstanceDuck;

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
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/SimpleReloadInstance$StateFactory;create(Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/server/packs/resources/PreparableReloadListener;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    <S> CompletableFuture<S> create(@Coerce Object sharedState, PreparableReloadListener.PreparationBarrier previousStep, ResourceManager resourceManager, PreparableReloadListener listener, Executor taskExecutor, Executor reloadExecutor, Operation<CompletableFuture<S>> original, @Local(argsOnly = true) CompletableFuture<Unit> initialTask) {
        if (initialTask instanceof PreparationBarrier) {
            this.animatedLoadingOverlay$initialTask = (PreparationBarrier) initialTask;
            return initialTask.thenCompose(ignored -> original.call(sharedState, previousStep, resourceManager, listener, taskExecutor, reloadExecutor));
        }
        return original.call(sharedState, previousStep, resourceManager, listener, taskExecutor, reloadExecutor);
    }
}
