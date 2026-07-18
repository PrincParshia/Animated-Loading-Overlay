package princ.animatedloadingoverlay.client.mixin.gui.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import princ.animatedloadingoverlay.client.duck.pack.resources.SimpleReloadInstanceDuck;
import princ.animatedloadingoverlay.client.sounds.SoundInstance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

import static princ.animatedloadingoverlay.client.Constants.*;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {

    @Shadow
    @Final
    private ReloadInstance reload;

    @Unique
    private long animatedLoadingOverlay$animationStart = -1L;

    @Unique
    private SoundInstance animatedLoadingOverlay$sound;


    @Inject(method = "<init>", at = @At("TAIL"))
    void animatedLoadingOverlay$init(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn, CallbackInfo callbackInfo) {
        if (this.reload instanceof SimpleReloadInstanceDuck reloadDuck) {
            reloadDuck.animatedLoadingOverlay$animationFinished(false);
        }
        SoundInstance sound = new SoundInstance();
        sound.load(minecraft, SOUND);
        this.animatedLoadingOverlay$sound = sound;
    }

    @Inject(method = "registerTextures", at = @At("HEAD"))
    private static void animatedLoadingOverlay$registerTextures(Minecraft minecraft, CallbackInfo callbackInfo) {
        minecraft.getTextureManager().register(BACKGROUND, new LogoTexture(BACKGROUND));
        for (ResourceLocation sheet : SHEETS) {
            minecraft.getTextureManager().register(sheet, new LogoTexture(sheet));
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V",
                    ordinal = 0
            )
    )
    void animatedLoadingOverlay$blit(GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, Operation<Void> original) {
        this.animatedLoadingOverlay$blit(graphics);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V",
                    ordinal = 1
            )
    )
    void animatedLoadingOverlay$wrapBlit(GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, Operation<Void> original) {
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"
            )
    )
    void fill(GuiGraphics graphics, RenderType renderType, int minX, int minY, int maxX, int maxY, int color, Operation<Void> original) {
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFunc(II)V"
            )
    )
    void animatedLoadingOverlay$blendFunc(int sourceFactor, int destFactor, Operation<Void> original) {
        RenderSystem.defaultBlendFunc();
    }


    @Unique
    void animatedLoadingOverlay$blit(GuiGraphics graphics) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        long now = Util.getMillis();

        if (this.animatedLoadingOverlay$animationStart == -1L) {
            this.animatedLoadingOverlay$animationStart = now;
            this.animatedLoadingOverlay$sound.play();
        }

        long frame = ((now - this.animatedLoadingOverlay$animationStart) * FPS) / 1000L;
        int frameIndex = (int) Math.min(frame, FRAMES - 1);

        int sheetIndex = frameIndex / FRAMES_PER_SHEET;
        int frameInSheet = frameIndex % FRAMES_PER_SHEET;

        ResourceLocation texture = SHEETS[sheetIndex];
        boolean lastSheet = sheetIndex == SHEET_COUNT - 1;

        int col = frameInSheet % COLS;
        int row = frameInSheet / COLS;

        float u = lastSheet ? 0.0F : col * FRAME_WIDTH;
        float v = lastSheet ? 0.0F : row * FRAME_HEIGHT;

        int textureWidth = lastSheet ? FRAME_WIDTH : SHEET_WIDTH;
        int textureHeight = lastSheet ? FRAME_HEIGHT : SHEET_HEIGHT;

        int width = Math.round(screenWidth * SCALE);
        int height = Math.round(screenHeight * SCALE);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;

        graphics.blit(BACKGROUND, 0, 0, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);
        graphics.blit(texture, x, y, width, height, u, v, FRAME_WIDTH, FRAME_HEIGHT, textureWidth, textureHeight);

        if (frameIndex >= FRAMES - 1) {
            if (this.reload instanceof SimpleReloadInstanceDuck reloadDuck) {
                reloadDuck.animatedLoadingOverlay$animationFinished(true);
            }
        }
    }

    private static class LogoTexture extends SimpleTexture {
        private final ResourceLocation texture;

        public LogoTexture(ResourceLocation texture) {
            super(texture);
            this.texture = texture;
        }

        @Override
        protected TextureImage getTextureImage(ResourceManager resourceManager) {
            String path = "assets/" + this.texture.getNamespace() + "/" + this.texture.getPath();
            try (InputStream resource = LogoTexture.class.getClassLoader().getResourceAsStream(path)) {
                if (resource == null) {
                    return new TextureImage(new FileNotFoundException(path));
                }
                return new TextureImage(new TextureMetadataSection(false, false), NativeImage.read(resource));
            } catch (IOException e) {
                return new TextureImage(e);
            }
        }
    }
}
