package com.minenorth_printer.client;

import com.minenorth_printer.ModRegistry;
import com.minenorth_printer.PrinterConfig;
import com.minenorth_printer.block.PrinterBlock;
import com.minenorth_printer.block.PrinterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** Gère le bruit de fonctionnement en boucle de chaque imprimante allumée (côté client uniquement). */
public final class PrinterSounds {
    private PrinterSounds() {}

    private static final Map<BlockPos, LoopSound> PLAYING = new HashMap<>();

    public static void ensurePlaying(PrinterBlockEntity be) {
        if (!PrinterConfig.get(PrinterConfig.RUNNING_SOUND)) return;
        BlockPos pos = be.getBlockPos().immutable();
        LoopSound current = PLAYING.get(pos);
        Minecraft mc = Minecraft.getInstance();
        if (current != null && !current.isStopped() && mc.getSoundManager().isActive(current)) return;
        LoopSound sound = new LoopSound(be);
        PLAYING.put(pos, sound);
        mc.getSoundManager().play(sound);
    }

    private static final class LoopSound extends AbstractTickableSoundInstance {
        private final PrinterBlockEntity be;
        private final float baseVolume;
        private int fade;

        LoopSound(PrinterBlockEntity be) {
            super(ModRegistry.PRINTER_RUNNING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.be = be;
            this.looping = true;
            this.delay = 0;
            this.baseVolume = (float) PrinterConfig.get(PrinterConfig.RUNNING_SOUND_VOLUME);
            this.volume = 0.01F;
            this.pitch = 1.0F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            BlockPos p = be.getBlockPos();
            this.x = p.getX() + 0.5;
            this.y = p.getY() + 0.5;
            this.z = p.getZ() + 0.5;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            BlockState state = be.getLevel() == null ? null : be.getLevel().getBlockState(be.getBlockPos());
            boolean running = !be.isRemoved() && state != null
                    && state.getBlock() instanceof PrinterBlock && state.getValue(PrinterBlock.LIT);
            // fondu d'entrée / de sortie sur 10 ticks pour éviter les coupures sèches
            fade = Math.max(0, Math.min(10, fade + (running ? 1 : -1)));
            volume = Math.max(0.01F, baseVolume * fade / 10F);
            if (!running && fade == 0) {
                PLAYING.remove(be.getBlockPos(), this);
                stop();
            }
        }
    }
}
