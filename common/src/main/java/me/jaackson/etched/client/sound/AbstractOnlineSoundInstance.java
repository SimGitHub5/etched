package me.jaackson.etched.client.sound;

import me.jaackson.etched.Etched;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class AbstractOnlineSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private final String url;
    private final String subtitle;
    private final int attenuationDistance;
    private final DownloadProgressListener progressListener;
    private boolean stopped;

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, SoundSource source, @Nullable DownloadProgressListener progressListener) {
        this(url, subtitle, 16, source, progressListener);
    }

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, int attenuationDistance, SoundSource source, @Nullable DownloadProgressListener progressListener) {
        super(new ResourceLocation(Etched.MOD_ID, DigestUtils.md5Hex(url)), source);
        this.url = url;
        this.subtitle = subtitle;
        this.attenuationDistance = attenuationDistance;
        this.progressListener = progressListener;
    }

    /**
     * Stops playing this sound.
     */
    public void stop() {
        this.stopped = true;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents weighedSoundEvents = new WeighedSoundEvents(this.getLocation(), this.subtitle);
        weighedSoundEvents.addSound(new OnlineSound(this.getLocation(), this.url, this.attenuationDistance, this.progressListener));
        this.sound = weighedSoundEvents.getSound();
        return weighedSoundEvents;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
    }

    public static class OnlineSound extends Sound {

        private final String url;
        private final DownloadProgressListener progressListener;

        public OnlineSound(ResourceLocation location, String url, int attenuationDistance, DownloadProgressListener progressListener) {
            super(location.toString(), 1.0F, 1.0F, 1, Type.FILE, true, false, attenuationDistance);
            this.url = url;
            this.progressListener = progressListener;
        }

        public String getURL() {
            return url;
        }

        public DownloadProgressListener getProgressListener() {
            return progressListener;
        }
    }
}