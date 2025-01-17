package gg.moonflower.etched.common.item;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ClientboundPlayMusicPacket;
import gg.moonflower.etched.common.network.play.handler.EtchedClientPlayPacketHandlerImpl;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.pollen.api.util.NbtConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Ocelot
 */
public class EtchedMusicDiscItem extends Item implements PlayableRecord {

    private static final Component ALBUM = new TranslatableComponent("item." + Etched.MOD_ID + ".etched_music_disc.album").withStyle(ChatFormatting.BLUE);

    public EtchedMusicDiscItem(Properties properties) {
        super(properties);
    }

    /**
     * Retrieves the music URL from the specified stack.
     *
     * @param stack The stack to get NBT from
     * @return The optional URL for that item
     */
    public static Optional<TrackData[]> getMusic(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || (!nbt.contains("Music", NbtConstants.COMPOUND) && !nbt.contains("Music", NbtConstants.LIST)))
            return Optional.empty();

        if (nbt.contains("Music", NbtConstants.LIST)) {
            ListTag musicNbt = nbt.getList("Music", NbtConstants.COMPOUND);
            TrackData[] data = new TrackData[musicNbt.size()];

            int valid = 0;
            for (int i = 0; i < musicNbt.size(); i++) {
                CompoundTag trackNbt = musicNbt.getCompound(i);
                if (TrackData.isValid(trackNbt)) {
                    Optional<TrackData> optional = TrackData.CODEC.parse(NbtOps.INSTANCE, trackNbt).result();
                    if (optional.isPresent())
                        data[valid++] = optional.get();
                }
            }

            if (valid == 0)
                return Optional.empty();
            if (valid >= data.length)
                return Optional.of(data);

            TrackData[] result = new TrackData[valid];
            System.arraycopy(data, 0, result, 0, result.length);
            return Optional.of(result);
        }

        return TrackData.isValid(nbt.getCompound("Music")) ? TrackData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("Music")).result().map(track -> new TrackData[]{track}) : Optional.empty();
    }

    public static Optional<TrackData> getAlbum(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Album", NbtConstants.COMPOUND) && !nbt.contains("Music", NbtConstants.LIST))
            return getMusic(stack).map(tracks -> tracks[0]);
        return TrackData.isValid(nbt.getCompound("Album")) ? TrackData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("Album")).result() : getMusic(stack).map(tracks -> tracks[0]);
    }

    public static int getTrackCount(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || (!nbt.contains("Music", NbtConstants.COMPOUND) && !nbt.contains("Music", NbtConstants.LIST)))
            return 0;

        if (nbt.contains("Music", NbtConstants.LIST)) {
            ListTag musicNbt = nbt.getList("Music", NbtConstants.COMPOUND);

            int valid = 0;
            for (int i = 0; i < musicNbt.size(); i++) {
                CompoundTag trackNbt = musicNbt.getCompound(i);
                if (TrackData.isValid(trackNbt))
                    valid++;
            }

            return valid;
        }

        return TrackData.isValid(nbt.getCompound("Music")) ? 1 : 0;
    }

    /**
     * Retrieves the label pattern from the specified stack.
     *
     * @param stack The stack to get the pattern from
     * @return The pattern for that item
     */
    public static LabelPattern getPattern(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Pattern", NbtConstants.ANY_NUMERIC))
            return LabelPattern.FLAT;
        int id = nbt.getByte("Pattern");
        return id < 0 || id >= LabelPattern.values().length ? LabelPattern.FLAT : LabelPattern.values()[id];
    }

    /**
     * Retrieves the color of the physical disc from the specified stack.
     *
     * @param stack The stack to get the color from
     * @return The color for the physical disc
     */
    public static int getDiscColor(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null)
            return 0x515151;

        // Convert old colors
        if (nbt.contains("PrimaryColor", NbtConstants.ANY_NUMERIC)) {
            nbt.putInt("DiscColor", nbt.getInt("PrimaryColor"));
            nbt.remove("PrimaryColor");
        }

        if (!nbt.contains("DiscColor", NbtConstants.ANY_NUMERIC))
            return 0x515151;
        return nbt.getInt("DiscColor");
    }

    /**
     * Retrieves the primary color of the label from the specified stack.
     *
     * @param stack The stack to get the color from
     * @return The color for the label
     */
    public static int getLabelPrimaryColor(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null)
            return 0xFFFFFF;

        // Convert old colors
        CompoundTag labelTag = nbt.getCompound("LabelColor");
        if (nbt.contains("SecondaryColor", NbtConstants.ANY_NUMERIC)) {
            labelTag.putInt("Primary", nbt.getInt("SecondaryColor"));
            labelTag.putInt("Secondary", nbt.getInt("SecondaryColor"));
            nbt.put("LabelColor", labelTag);
            nbt.remove("SecondaryColor");
        }

        return labelTag.contains("Primary", NbtConstants.ANY_NUMERIC) ? labelTag.getInt("Primary") : 0xFFFFFF;
    }

    /**
     * Retrieves the secondary color of the label from the specified stack.
     *
     * @param stack The stack to get the color from
     * @return The color for the label
     */
    public static int getLabelSecondaryColor(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null)
            return 0xFFFFFF;

        // Convert old colors
        CompoundTag labelTag = nbt.getCompound("LabelColor");
        if (nbt.contains("SecondaryColor", NbtConstants.ANY_NUMERIC)) {
            labelTag.putInt("Primary", nbt.getInt("SecondaryColor"));
            labelTag.putInt("Secondary", nbt.getInt("SecondaryColor"));
            nbt.put("LabelColor", labelTag);
            nbt.remove("SecondaryColor");
        }

        return labelTag.contains("Secondary", NbtConstants.ANY_NUMERIC) ? labelTag.getInt("Secondary") : 0xFFFFFF;
    }

    /**
     * Sets the URL for the specified stack.
     *
     * @param stack  The stack to set NBT for
     * @param tracks The tracks to apply to the disk. If more than one are provided, the first is treated as the album data
     */
    public static void setMusic(ItemStack stack, TrackData... tracks) {
        if (tracks.length == 0) {
            if (stack.getTag() != null) {
                stack.getTag().remove("Music");
                stack.getTag().remove("Album");
            }
        } else if (tracks.length == 1) {
            CompoundTag nbt = stack.getOrCreateTag();
            nbt.put("Music", tracks[0].save(new CompoundTag()));
            nbt.remove("Album");
        } else {
            ListTag musicNbt = new ListTag();
            for (int i = 1; i < tracks.length; i++)
                musicNbt.add(tracks[i].save(new CompoundTag()));
            CompoundTag nbt = stack.getOrCreateTag();
            nbt.put("Music", musicNbt);
            nbt.put("Album", tracks[0].save(new CompoundTag()));
        }
    }

    /**
     * Sets the pattern for the specified stack.
     *
     * @param stack   The stack to set NBT for
     * @param pattern The pattern to apply to the disk or <code>null</code> to remove and default to {@link LabelPattern#FLAT}
     */
    public static void setPattern(ItemStack stack, @Nullable LabelPattern pattern) {
        if (pattern == null) {
            if (stack.getTag() != null)
                stack.getTag().remove("Pattern");
        } else {
            stack.getOrCreateTag().putByte("Pattern", (byte) pattern.ordinal());
        }
    }

    /**
     * Sets the color for the specified stack.
     *
     * @param stack          The stack to set NBT for
     * @param primaryColor   The color to use for the physical disk
     * @param secondaryColor The color to use for the label
     */
    public static void setColor(ItemStack stack, int discColor, int primaryColor, int secondaryColor) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("DiscColor", discColor);

        CompoundTag labelTag = tag.getCompound("LabelColor");
        labelTag.putInt("Primary", primaryColor);
        labelTag.putInt("Secondary", secondaryColor);
        tag.put("LabelColor", labelTag);
    }

    @Override
    public boolean canPlay(ItemStack stack) {
        return getMusic(stack).isPresent();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Optional<SoundInstance> createEntitySound(ItemStack stack, Entity entity, int track) {
        return track < 0 ? Optional.empty() : getMusic(stack).filter(tracks -> track < tracks.length).map(tracks -> EtchedClientPlayPacketHandlerImpl.getEtchedRecord(tracks[track].getUrl(), tracks[track].getDisplayName(), entity));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        getAlbum(stack).ifPresent(track -> {
            list.add(track.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            SoundSourceManager.getBrandText(track.getUrl()).ifPresent(list::add);
            if (getTrackCount(stack) > 1)
                list.add(ALBUM);
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD))
            return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        Optional<TrackData[]> optional = getMusic(stack);
        if (!optional.isPresent())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            ((JukeboxBlock) Blocks.JUKEBOX).setRecord(level, pos, state, stack);
            EtchedMessages.PLAY.sendToNear((ServerLevel) level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64, new ClientboundPlayMusicPacket(optional.get(), pos));
            stack.shrink(1);
            Player player = ctx.getPlayer();
            if (player != null)
                player.awardStat(Stats.PLAY_RECORD);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }


    /**
     * @author Jackson
     */
    public enum LabelPattern {

        FLAT, CROSS, EYE, PARALLEL, STAR, GOLD(true);

        private final boolean simple;
        private final Pair<ResourceLocation, ResourceLocation> textures;

        LabelPattern() {
            this(false);
        }

        LabelPattern(boolean simple) {
            this.simple = simple;
            this.textures = Pair.of(
                    new ResourceLocation(Etched.MOD_ID, "textures/item/" + this.name().toLowerCase(Locale.ROOT) + "_label" + (simple ? "" : "_top") + ".png"),
                    new ResourceLocation(Etched.MOD_ID, "textures/item/" + this.name().toLowerCase(Locale.ROOT) + "_label" + (simple ? "" : "_bottom") + ".png")
            );
        }

        /**
         * @return A pair of {@link ResourceLocation} for a top and bottom texture. If the pattern is simple, both locations are the same.
         */
        public Pair<ResourceLocation, ResourceLocation> getTextures() {
            return textures;
        }

        /**
         * @return Whether the label pattern supports two colors.
         */
        public boolean isSimple() {
            return simple;
        }

        /**
         * @return Whether this label can be colored
         */
        public boolean isColorable() {
            return this != GOLD;
        }
    }
}
