package princ.animatedloadingoverlay.client.duck.pack.resources;

import net.minecraft.util.Unit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface SimpleReloadInstanceDuck {

    final class PreparationBarrier extends CompletableFuture<Unit> {

        public PreparationBarrier() {
            this.completeOnTimeout(Unit.INSTANCE, 5, TimeUnit.SECONDS);
        }
    }

    boolean animatedLoadingOverlay$animationFinished();
    void animatedLoadingOverlay$animationFinished(boolean finished);
}
