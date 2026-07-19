package princ.animatedloadingoverlay.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ClientHooks.class)
public class ClientHooksMixin {

    @WrapMethod(method = "createLoadingOverlay")
    private static Overlay animatedLoadingOverlay$createLoadingOverlay(Minecraft minecraft, ReloadInstance reloadInstance, Consumer<Optional<Throwable>> errorHandler, boolean fadeIn, Operation<Overlay> original) {
        if (!(minecraft.getOverlay() instanceof LoadingOverlay)) {
            return new LoadingOverlay(minecraft, reloadInstance, errorHandler, fadeIn);
        }
        return original.call(minecraft, reloadInstance, errorHandler, fadeIn);
    }
}
