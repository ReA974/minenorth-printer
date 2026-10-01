package com.minenorth.printer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.concurrent.CompletableFuture;

import static com.minenorth.printer.PrinterConfig.BREAK_RADIUS;
import static com.minenorth.printer.PrinterConfig.TIME_TO_BREAK;

@Mod.EventBusSubscriber(modid = PrinterMod.MODID)
public class PrinterCommand {

    @FunctionalInterface
    private interface Exec {
        int run(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException;
    }

    private static Command<CommandSourceStack> guard(Exec exec) {
        return ctx -> {
            CommandSourceStack src = ctx.getSource();
            if (!src.hasPermission(PrinterConfig.PERMISSION_LEVEL.get())) {
                src.sendFailure(TextUtil.color(PrinterConfig.MSG_NO_PERM.get()));
                return 0;
            }
            return exec.run(ctx, src.getPlayerOrException());
        };
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("printer").executes(guard(PrinterCommand::help));


        root.then(Commands.literal("help").executes(guard(PrinterCommand::help)));
        root.then(Commands.literal("info").executes(guard(PrinterCommand::info)));
        root.then(Commands.literal("give").executes(guard(PrinterCommand::give)));
        root.then(Commands.literal("speed").then(Commands.literal("set")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1))
                            .executes(guard(PrinterCommand::setSpeed)))));
        root.then(Commands.literal("power").then(Commands.literal("set")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(guard(PrinterCommand::setPower)))));

        e.getDispatcher().register(root);
    }

    // ------------------------------------------------------------------

    private static BlockPos targetedPrinter(ServerPlayer p) {
        HitResult hit = p.pick(16.0D, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (PrinterConfig.isPrinterBlock(p.serverLevel().getBlockState(pos))) return pos;
        }
        TextUtil.msg(p, "&cRegarde un Printer pour utiliser cette commande.");
        return null;
    }

    private static int help(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        String prefix = PrinterConfig.PREFIX.get();
        p.sendSystemMessage(TextUtil.color(""));
        p.sendSystemMessage(TextUtil.color("&4=&a-&4=&a-&4=" + prefix + "&a-&4=&a-&4=&a-"));
        p.sendSystemMessage(TextUtil.color(""));
        p.sendSystemMessage(TextUtil.color("&7-> &a/Printer speed set (Number) &b[&3Pour Modifier la vitesse du Printer&b]"));
        p.sendSystemMessage(TextUtil.color("&7-> &a/Printer Power set (Number) &b[&3Pour Modifier la Batterie du Printer&b]"));
        p.sendSystemMessage(TextUtil.color("&7-> &a/Printer Info &b[&3Pour Voir les Information du Printer&b]"));
        p.sendSystemMessage(TextUtil.color("&7-> &a/Printer Give &b[&3Give tous les outils et les Printers&b]"));
        p.sendSystemMessage(TextUtil.color("&7-> &a/Printer Help &b[&3Pour Avoir cette Aide&b]"));
        p.sendSystemMessage(TextUtil.color(""));
        p.sendSystemMessage(TextUtil.color("&4=&a-&4=&a-&4=" + prefix + "&a-&4=&a-&4=&a-"));
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        BlockPos pos = targetedPrinter(p);
        if (pos == null) return 0;
        PrinterData data = PrinterData.get(p.serverLevel());
        PrinterEvents.sendInfo(p, pos, data.getOrCreate(pos));
        return 1;
    }

    private static int breakPrinter(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        BlockPos pos = targetedPrinter(p);
        if (pos == null) return 0;

        ServerLevel level = p.serverLevel();

        // 1. Alerte les joueurs situés dans un rayon de 10 blocs autour du printer
        double radius = BREAK_RADIUS.get().longValue();
        double radiusSq = radius * radius;
        long timeToBreak = TIME_TO_BREAK.get()*100;
        Vec3 center = pos.getCenter();

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSq) {
                player.sendSystemMessage(
                        Component.literal("⚠ " + p.getScoreboardName() + " tente de détruire un printer près de vous ! Destruction dans "+ timeToBreak+"s.")
                );
            }
        }

        // 2. Planifie la destruction 20 secondes plus tard (400 ticks Minecraft = 20s)
        // Remarque : Si tu utilises un modloader (NeoForge / Forge), tu peux utiliser un Event Tick ou l'exécuteur du serveur.
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(timeToBreak); // Attend 20 secondes de manière asynchrone
            } catch (InterruptedException ignored) {}
        }).thenAcceptAsync(v -> {
            // Exécution sur le thread principal du serveur pour modifier le monde en sécurité
            level.getServer().execute(() -> {
                // Vérifie si le bloc à cette position est toujours valide avant de le casser
                PrinterData.get(level).remove(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            });
        });

        return 1;
    }

    private static int setSpeed(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        BlockPos pos = targetedPrinter(p);
        if (pos == null) return 0;
        if (v > PrinterConfig.MAX_SPEED.get()) {
            TextUtil.msg(p, "&cLa vitesse maximum est de " + PrinterConfig.MAX_SPEED.get() + "x !");
            return 0;
        }
        PrinterData data = PrinterData.get(p.serverLevel());
        data.getOrCreate(pos).speed = v;
        data.setDirty();
        TextUtil.msg(p, "tu viens de mettre la vitesse à " + TextUtil.num(v) + "x (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
        return 1;
    }

    private static int setPower(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        int v = IntegerArgumentType.getInteger(ctx, "value");
        BlockPos pos = targetedPrinter(p);
        if (pos == null) return 0;
        PrinterData data = PrinterData.get(p.serverLevel());
        data.getOrCreate(pos).power = v;
        data.setDirty();
        TextUtil.msg(p, "tu viens de mettre " + v + "% de batterie (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        giveNamed(p, PrinterConfig.BATTERY_ITEM, 1, PrinterConfig.BATTERY_NAME_1.get());
        giveNamed(p, PrinterConfig.BATTERY_ITEM, 1, PrinterConfig.BATTERY_NAME_2.get());
        giveNamed(p, PrinterConfig.BATTERY_ITEM, 1, PrinterConfig.BATTERY_NAME_3.get());
        giveNamed(p, PrinterConfig.CAPACITY_ITEM, 16, "&a[&eCapacité Batterie +" + PrinterConfig.CAPACITY_STEP.get() + "%&a]");
        giveNamed(p, PrinterConfig.SPEED_ITEM, 16, "&a[&eVitesse +1&a]");

        Item printerItem = PrinterConfig.block().asItem();
        if (printerItem == Items.AIR) {
            TextUtil.msg(p, "&cprinterBlock introuvable dans la config.");
        } else {
            for (PrinterConfig.ClassCfg c : PrinterConfig.CLASSES) {
                ItemStack st = new ItemStack(printerItem);
                st.setHoverName(TextUtil.color(c.itemName.get()));
                ItemHandlerHelper.giveItemToPlayer(p, st);
            }
        }
        giveNamed(p, PrinterConfig.INK_ITEM, 16, "&7Printer &f[&bEncre Printer&f]");
        return 1;
    }

    private static void giveNamed(ServerPlayer p, ForgeConfigSpec.ConfigValue<String> itemCfg, int count, String name) {
        Item it = PrinterConfig.item(itemCfg.get());
        if (it == Items.AIR) {
            TextUtil.msg(p, "&cItem introuvable dans la config : " + itemCfg.get());
            return;
        }
        ItemStack st = new ItemStack(it, count);
        st.setHoverName(TextUtil.color(name));
        ItemHandlerHelper.giveItemToPlayer(p, st);
    }
}
