package princ.animatedloadingoverlay.client.mixin.pack.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import princ.animatedloadingoverlay.client.duck.pack.resources.SimpleReloadInstanceDuck;

import java.util.concurrent.CompletableFuture;

@Mixin(targets = "net/minecraft/server/packs/resources/SimpleReloadInstance$1")
public class PreparationBarrierMixin {

    @Shadow
    @Final
    SimpleReloadInstance<?> this$0;

    @WrapOperation(
            method = "lambda$wait$0",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;complete(Ljava/lang/Object;)Z"
            )
    )
    boolean animatedLoadingOverlay$delayCompletion(CompletableFuture<Unit> allPreparations, Object value, Operation<Boolean> original) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getOverlay() instanceof LoadingOverlay) {
            SimpleReloadInstanceDuck duck = (SimpleReloadInstanceDuck) this.this$0;
            if (!duck.animatedLoadingOverlay$animationFinished()) {
                return true;
            }
        }

        return original.call(allPreparations, value);
    }
}
