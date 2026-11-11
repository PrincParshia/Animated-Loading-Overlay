package princ.animatedloadingoverlay.client.duck.pack.resources;

import net.minecraft.util.Unit;

import java.util.concurrent.CompletableFuture;

public interface SimpleReloadInstanceDuck {
    CompletableFuture<Unit> animatedLoadingOverlay$allPreparations();
    boolean animatedLoadingOverlay$animationFinished();
    void animatedLoadingOverlay$animationFinished(boolean finished);
}
