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
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
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
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import static princ.animatedloadingoverlay.client.Constants.*;

@Mixin(value = LoadingOverlay.class, priority = 5000)
public class LoadingOverlayMixin {

    @Shadow
    @Final
    private static int LOGO_BACKGROUND_COLOR;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private long fadeOutStart;

    @Shadow
    private long fadeInStart;

    @Unique
    private long animatedLoadingOverlay$progressBarFadeInStart = -1L;

    @Shadow
    private static int replaceAlpha(int color, int alpha) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Unique
    long animatedLoadingOverlay$animationStart = -1L;

    @Unique
    SoundInstance animatedLoadingOverlay$sound;

    @Inject(method = "<init>", at = @At("TAIL"))
    void animatedLoadingOverlay$init(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn, CallbackInfo callbackInfo) {
        SoundInstance sound = new SoundInstance();
        sound.load(minecraft, SOUND);
        this.animatedLoadingOverlay$sound = sound;
    }

    @Inject(method = "registerTextures", at = @At("HEAD"))
    private static void animatedLoadingOverlay$registerTextures(Minecraft minecraft, CallbackInfo callbackInfo) {
        for (ResourceLocation sheet : SHEETS) {
            minecraft.getTextureManager().register(sheet, new LogoTexture(sheet));
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"
            )
    )
    void animatedLoadingOverlay$fill(GuiGraphics graphics, RenderType renderType, int minX, int minY, int maxX, int maxY, int color, Operation<Void> original) {
        this.animatedLoadingOverlay$fill(graphics);
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
    void animatedLoadingOverlay$skipBlit(GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, Operation<Void> original) {
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"
            )
    )
    void animatedLoadingOverlay$extractProgressBar(LoadingOverlay instance, GuiGraphics graphics, int x0, int y0, int x1, int y1, float fade, Operation<Void> original) {
        long now = Util.getMillis();

        boolean fadeIn = this.reload instanceof SimpleReloadInstanceDuck reloadDuck && reloadDuck.animatedLoadingOverlay$animationFinished();
        float alpha;

        if (fadeIn) {
            if (this.animatedLoadingOverlay$progressBarFadeInStart == -1L) {
                this.animatedLoadingOverlay$progressBarFadeInStart = now;
            }
            float fadeInAnim = this.animatedLoadingOverlay$progressBarFadeInStart > -1L ? (float) (now - this.animatedLoadingOverlay$progressBarFadeInStart) / 500.0F : -1.0F;
            alpha = Mth.clamp(fadeInAnim, 0.0F, 1.0F);
        } else {
            alpha = 0.0F;
        }

        original.call(instance, graphics, x0, y0, x1, y1, fade * alpha);
    }

    @Unique
    void animatedLoadingOverlay$fill(GuiGraphics graphics) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        long now = Util.getMillis();

        int height = (int) (Math.min(screenWidth / ASPECT_RATIO, screenHeight) * SCALE);
        int width = (int) (height * ASPECT_RATIO);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;

        float fadeOutAnim = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAnim = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 500.0F : -1.0F;
        int alpha;

        if (fadeOutAnim >= 1.0F) {
            alpha = Mth.ceil((1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F)) * 255);
        } else if (this.fadeIn) {
            alpha = Mth.ceil(Mth.clamp(fadeInAnim, 0.0F, 1.0F) * 255);
        } else {
            alpha = 255;
        }

        int color = replaceAlpha(LOGO_BACKGROUND_COLOR, alpha);

        graphics.fill(0, 0, screenWidth, y, color);
        graphics.fill(0, y + height, screenWidth, screenHeight, color);
        graphics.fill(0, y, x, y + height, color);
        graphics.fill(x + width, y, screenWidth, y + height, color);
    }

    @Unique
    void animatedLoadingOverlay$blit(GuiGraphics graphics) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        long now = Util.getMillis();

        if (this.animatedLoadingOverlay$animationStart == -1L) {
            this.animatedLoadingOverlay$animationStart = now;
            this.animatedLoadingOverlay$sound.play();
            if (this.reload instanceof SimpleReloadInstanceDuck reloadDuck) {
                reloadDuck.animatedLoadingOverlay$animationFinished(false);
            }
        }

        long frame = ((now - this.animatedLoadingOverlay$animationStart) * FPS) / 1000L;
        int frameIndex = (int) Math.min(frame, FRAMES - 1);

        int sheetIndex = frameIndex / FRAMES_PER_SHEET;
        int frameInSheet = frameIndex % FRAMES_PER_SHEET;

        ResourceLocation texture = SHEETS[sheetIndex];
        boolean endSheet = sheetIndex == SHEET_COUNT - 1;

        int col = frameInSheet % COLS;
        int row = frameInSheet / COLS;

        float u = endSheet ? 0.0F : col * FRAME_WIDTH;
        float v = endSheet ? 0.0F : row * FRAME_HEIGHT;

        int textureWidth = endSheet ? FRAME_WIDTH : SHEET_WIDTH;
        int textureHeight = endSheet ? FRAME_HEIGHT : SHEET_HEIGHT;

        int height = (int) (Math.min(screenWidth / ASPECT_RATIO, screenHeight) * SCALE);
        int width = (int) (height * ASPECT_RATIO);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;

        graphics.blit(texture, x, y, width, height, u, v, FRAME_WIDTH, FRAME_HEIGHT, textureWidth, textureHeight);

        if (frameIndex >= FRAMES - 1) {
            if (this.reload instanceof SimpleReloadInstanceDuck reloadDuck) {
                reloadDuck.animatedLoadingOverlay$animationFinished(true);
            }
        }
    }

    static class LogoTexture extends SimpleTexture {
        private final ResourceLocation texture;

        public LogoTexture(ResourceLocation texture) {
            super(texture);
            this.texture = texture;
        }

        @Override
        public TextureImage getTextureImage(ResourceManager resourceManager) {
            Path path = Path.of("assets/", this.texture.getNamespace(), this.texture.getPath());
            try (InputStream resource = this.getClass().getClassLoader().getResourceAsStream(path.toString())) {
                if (resource == null) {
                    return new TextureImage(new FileNotFoundException());
                }
                return new TextureImage(new TextureMetadataSection(true, true), NativeImage.read(resource));
            } catch (IOException ioException) {
                return new TextureImage(ioException);
            }
        }
    }
}
