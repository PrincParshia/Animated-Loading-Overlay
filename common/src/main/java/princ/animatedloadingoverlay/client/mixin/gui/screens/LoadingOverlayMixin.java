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
import net.minecraft.util.ARGB;
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
    private static void animatedLoadingOverlay$registerTextures(TextureManager textureManager, CallbackInfo callbackInfo) {
        textureManager.registerAndLoad(BACKGROUND, new LogoTexture(BACKGROUND));
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
    void animatedLoadingOverlay$cancelBlit(GuiGraphicsExtractor graphics, RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color, Operation<Void> original) {
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
            )
    )
    void animatedLoadingOverlay$fill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int col, Operation<Void> original) {
    }

    @Unique
    void animatedLoadingOverlay$blit(GuiGraphicsExtractor graphics, int color) {
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight, screenWidth, screenHeight, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, FRAME_WIDTH, FRAME_HEIGHT, textureWidth, textureHeight, color);

        if (frameIndex >= FRAMES - 1) {
            if (this.reload instanceof SimpleReloadInstanceDuck reloadDuck) {
                reloadDuck.animatedLoadingOverlay$animationFinished(true);
            }
        }
    }

    private static class LogoTexture extends ReloadableTexture {
        private final Identifier texture;

        public LogoTexture(Identifier texture) {
            super(texture);
            this.texture = texture;
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            String path = "assets/" + this.texture.getNamespace() + "/" + this.texture.getPath();
            try (InputStream resource = LogoTexture.class.getClassLoader().getResourceAsStream(path)) {
                if (resource == null) {
                    throw new IOException();
                }
                return new TextureContents(NativeImage.read(resource), new TextureMetadataSection(false, false, MipmapStrategy.MEAN, 0.0F));
            }
        }
    }
}
