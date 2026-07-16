package princ.animatedloadingoverlay.client.mixin.pack.resources;

import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import princ.animatedloadingoverlay.duck.pack.resources.SimpleReloadInstanceDuck;

import java.util.concurrent.CompletableFuture;

@Mixin(SimpleReloadInstance.class)
public class SimpleReloadInstanceMixin implements SimpleReloadInstanceDuck {

    @Shadow
    @Final
    private CompletableFuture<Unit> allPreparations;

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
            if (!this.allPreparations.isDone()) {
                this.allPreparations.complete(Unit.INSTANCE);
            }
        }
    }
}
