package princ.animatedloadingoverlay.client.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundInstance {
    public Clip clip;

    public void play() {
        if (this.clip != null) {
            this.clip.setFramePosition(0);
            this.clip.start();
        }
    }

    public void stop() {
        if (this.clip != null) {
            this.clip.stop();
        }
    }

    public void loadAsync(Minecraft minecraft, ResourceLocation soundId, String name) {
        Thread thread = new Thread(() -> load(minecraft, soundId), name);
        thread.setDaemon(true);
        thread.start();
    }

    public void load(Minecraft minecraft, ResourceLocation soundId) {
        minecraft.getResourceManager().getResource(soundId).ifPresent(resource -> {
            try (InputStream inputStream = resource.open(); BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream); AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);

                FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float masterVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
                float uiVolume = minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
                float linearVolume = masterVolume * uiVolume;

                if (linearVolume <= 0.0001F) {
                    volume.setValue(volume.getMinimum());
                    return;
                }

                float dB = Mth.clamp((float) (20.0F * Math.log10(linearVolume)), volume.getMinimum(), volume.getMaximum());
                volume.setValue(dB);

                this.clip = clip;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
