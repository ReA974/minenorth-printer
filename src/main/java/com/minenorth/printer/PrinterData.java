package com.minenorth.printer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** État persistant des Printers d'une dimension (équivalent des variables Skript {Speed.%loc%}, etc.). */
public class PrinterData extends SavedData {

    public static class State {
        public String cls = "Silver";
        public double speed = 1.0;
        public int power = 100;
        public int powerMax = 100;
        public UUID owner;
        public int ink = 0;
        public boolean running = false;
        public int progress = 0;
        public double acc = 0.0;
        public int stage = 0;
        public UUID runner;

        public static State fresh() {
            State s = new State();
            s.power = PrinterConfig.DEFAULT_CAPACITY.get();
            s.powerMax = s.power;
            return s;
        }
    }

    private final Map<Long, State> printers = new HashMap<>();

    public static PrinterData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PrinterData::load, PrinterData::new, "printer_data");
    }

    public State get(BlockPos pos) {
        return printers.get(pos.asLong());
    }

    public State getOrCreate(BlockPos pos) {
        State s = printers.get(pos.asLong());
        if (s == null) {
            s = State.fresh();
            printers.put(pos.asLong(), s);
            setDirty();
        }
        return s;
    }

    public void put(BlockPos pos, State s) {
        printers.put(pos.asLong(), s);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (printers.remove(pos.asLong()) != null) setDirty();
    }

    public Map<Long, State> all() {
        return printers;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, State> en : printers.entrySet()) {
            State s = en.getValue();
            CompoundTag t = new CompoundTag();
            t.putLong("pos", en.getKey());
            t.putString("cls", s.cls);
            t.putDouble("speed", s.speed);
            t.putInt("power", s.power);
            t.putInt("powerMax", s.powerMax);
            t.putInt("ink", s.ink);
            t.putBoolean("running", s.running);
            t.putInt("progress", s.progress);
            t.putDouble("acc", s.acc);
            t.putInt("stage", s.stage);
            if (s.owner != null) t.putUUID("owner", s.owner);
            if (s.runner != null) t.putUUID("runner", s.runner);
            list.add(t);
        }
        tag.put("printers", list);
        return tag;
    }

    public static PrinterData load(CompoundTag tag) {
        PrinterData d = new PrinterData();
        ListTag list = tag.getList("printers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            State s = new State();
            s.cls = t.getString("cls");
            s.speed = t.getDouble("speed");
            s.power = t.getInt("power");
            s.powerMax = t.getInt("powerMax");
            s.ink = t.getInt("ink");
            s.running = t.getBoolean("running");
            s.progress = t.getInt("progress");
            s.acc = t.getDouble("acc");
            s.stage = t.getInt("stage");
            if (t.hasUUID("owner")) s.owner = t.getUUID("owner");
            if (t.hasUUID("runner")) s.runner = t.getUUID("runner");
            d.printers.put(t.getLong("pos"), s);
        }
        return d;
    }
}
