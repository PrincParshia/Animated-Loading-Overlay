package princ.animatedloadingoverlay.client.mixin.pack.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import princ.animatedloadingoverlay.duck.pack.resources.SimpleReloadInstanceDuck;

import java.util.concurrent.CompletableFuture;

@Debug(export = true)
@Mixin(targets = "net/minecraft/server/packs/resources/SimpleReloadInstance$1")
public class PreparationBarrierMixin {

    @Shadow
    @Final
    SimpleReloadInstance<?> field_18053;

    @WrapOperation(
            method = "method_18374",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;complete(Ljava/lang/Object;)Z"
            )
    )
    boolean animatedLoadingOverlay$delayCompletion(CompletableFuture<Unit> allPreparations, Object value, Operation<Boolean> original) {
        SimpleReloadInstanceDuck duck = (SimpleReloadInstanceDuck) this.field_18053;

        if (!duck.animatedLoadingOverlay$animationFinished()) {
            return true;
        }

        return original.call(allPreparations, value);
    }
}
