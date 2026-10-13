package com.minenorth_printer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Envoie les valeurs de la config du serveur au joueur qui se connecte (la config COMMON n'est pas synchronisée par Forge). */
final class PrinterNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthPrinter.MODID, "config"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private PrinterNetwork() {}

    /** Forge indexe les messages par classe exacte : on ne peut pas enregistrer Map.class et envoyer un LinkedHashMap. */
    record ConfigMessage(Map<String, String> values) {}

    static void register() {
        CHANNEL.registerMessage(0, ConfigMessage.class, PrinterNetwork::write, PrinterNetwork::read, PrinterNetwork::handle);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp)
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ConfigMessage(PrinterConfig.snapshot()));
        });
    }

    private static void write(ConfigMessage msg, FriendlyByteBuf buf) {
        Map<String, String> m = msg.values();
        buf.writeVarInt(m.size());
        for (Map.Entry<String, String> e : m.entrySet()) {
            buf.writeUtf(String.valueOf(e.getKey()));
            buf.writeUtf(String.valueOf(e.getValue()));
        }
    }

    private static ConfigMessage read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) m.put(buf.readUtf(), buf.readUtf());
        return new ConfigMessage(m);
    }

    private static void handle(ConfigMessage msg, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PrinterConfig.setRemote(msg.values()));
        ctx.get().setPacketHandled(true);
    }
}
