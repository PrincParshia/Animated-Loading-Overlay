package princ.animatedloadingoverlay.client.mixin.gui.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
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
    private static void animatedLoadingOverlay$registerTextures(TextureManager textureManager, CallbackInfo callbackInfo) {
        for (Identifier sheet : SHEETS) {
            textureManager.registerAndLoad(sheet, new LogoTexture(sheet));
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
                    ordinal = 0
            )
    )
    void animatedLoadingOverlay$blit(GuiGraphicsExtractor graphics, RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color, Operation<Void> original) {
        this.animatedLoadingOverlay$blit(graphics, color);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
                    ordinal = 1
            )
    )
    void animatedLoadingOverlay$skipBlit(GuiGraphicsExtractor graphics, RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color, Operation<Void> original) {
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
            )
    )
    void animatedLoadingOverlay$fill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int col, Operation<Void> original) {
        this.animatedLoadingOverlay$fill(graphics);
    }

    @Unique
    void animatedLoadingOverlay$blit(GuiGraphicsExtractor graphics, int color) {
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

        Identifier texture = SHEETS[sheetIndex];
        boolean endSheet = sheetIndex == SHEET_COUNT - 1;

        int col = frameInSheet % COLS;
        int row = frameInSheet / COLS;

        float u = endSheet ? 0f : col * FRAME_WIDTH;
        float v = endSheet ? 0f : row * FRAME_HEIGHT;

        int textureWidth = endSheet ? FRAME_WIDTH : SHEET_WIDTH;
        int textureHeight = endSheet ? FRAME_HEIGHT : SHEET_HEIGHT;

        int width = Math.round(screenWidth * SCALE);
        int height = Math.round(screenHeight * SCALE);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, FRAME_WIDTH, FRAME_HEIGHT, textureWidth, textureHeight, color);

        if (frameIndex >= FRAMES - 1) {
            if (this.reload instanceof SimpleReloadInstanceDuck reloadDuck) {
                reloadDuck.animatedLoadingOverlay$animationFinished(true);
            }
        }
    }

    @Unique
    void animatedLoadingOverlay$fill(GuiGraphicsExtractor graphics) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        long now = Util.getMillis();

        int width = Math.round(screenWidth * SCALE);
        int height = Math.round(screenHeight * SCALE);
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

    static class LogoTexture extends ReloadableTexture {
        private final Identifier texture;

        public LogoTexture(Identifier texture) {
            super(texture);
            this.texture = texture;
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            Path path = Path.of("assets/", this.texture.getNamespace(), this.texture.getPath());
            try (InputStream resource = this.getClass().getClassLoader().getResourceAsStream(path.toString())) {
                if (resource == null) {
                    throw new FileNotFoundException();
                }
                return new TextureContents(NativeImage.read(resource), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
            }
        }
    }
}
