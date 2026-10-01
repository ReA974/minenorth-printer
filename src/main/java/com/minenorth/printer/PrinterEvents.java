package com.minenorth.printer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.minenorth.printer.PrinterConfig.BREAK_RADIUS;
import static com.minenorth.printer.PrinterConfig.TIME_TO_BREAK;

@Mod.EventBusSubscriber(modid = PrinterMod.MODID)
public class PrinterEvents {

    /** Classe de l'item Printer tenu au moment du clic (le nom est perdu une fois l'item posé). "" = non renommé. */
    private static final Map<UUID, String> PENDING_CLASS = new HashMap<>();

    // ------------------------------------------------------------------ utilitaires

    public static void sound(ServerLevel level, BlockPos pos, String id, float vol, float pitch) {
        SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        if (se != null) level.playSound(null, pos, se, SoundSource.BLOCKS, vol, pitch);
    }

    public static void sound(ServerPlayer p, String id, float pitch) {
        SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        if (se != null) p.playNotifySound(se, SoundSource.PLAYERS, 1.0f, pitch);
    }

    public static void sendInfo(ServerPlayer p, BlockPos pos, PrinterData.State s) {
        TextUtil.msg(p, "&3--- Information du Printer ---");
        TextUtil.msg(p, "&3Coordonnées: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        TextUtil.msg(p, "&3Classe: &e" + s.cls);
        TextUtil.msg(p, "&3Vitesse: " + TextUtil.num(s.speed) + "x &7(Max " + PrinterConfig.MAX_SPEED.get() + "x)");
        TextUtil.msg(p, "&3Batterie: " + s.power + "% / " + s.powerMax + "%");
        if (s.running) {
            TextUtil.msg(p, s.stage == 0 ? "&3Étape actuelle: En cours de lancement..." : "&3Étape actuelle: " + s.stage + "%");
        } else {
            TextUtil.msg(p, "&3Étape actuelle: En attente d'encre (" + s.ink + "/" + PrinterConfig.INK_NEEDED.get() + ")");
        }
    }

    // ------------------------------------------------------------------ démarrage

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        if (PrinterConfig.block() == Blocks.AIR)
            PrinterMod.LOGGER.warn("[Printer] printerBlock '{}' introuvable : le mod est inactif tant que le bloc n'existe pas.", PrinterConfig.PRINTER_BLOCK.get());
        warnItem("billItem", PrinterConfig.BILL_ITEM.get());
        warnItem("inkItem", PrinterConfig.INK_ITEM.get());
        warnItem("batteryItem", PrinterConfig.BATTERY_ITEM.get());
        warnItem("capacityUpgradeItem", PrinterConfig.CAPACITY_ITEM.get());
        warnItem("speedUpgradeItem", PrinterConfig.SPEED_ITEM.get());
    }

    private static void warnItem(String key, String id) {
        if (PrinterConfig.item(id) == Items.AIR)
            PrinterMod.LOGGER.warn("[Printer] {} = '{}' est introuvable (vérifie le fichier printer-common.toml).", key, id);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PENDING_CLASS.remove(e.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ pose

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof ServerPlayer player)) return;
        if (!PrinterConfig.isPrinterBlock(e.getPlacedBlock())) return;

        BlockPos pos = e.getPos();
        String cls = PENDING_CLASS.remove(player.getUUID());
        boolean named = cls != null && !cls.isEmpty();
        if (!named) cls = "Silver";

        PrinterData data = PrinterData.get(level);
        PrinterData.State s = PrinterData.State.fresh();
        s.cls = cls;
        s.owner = player.getUUID();
        data.put(pos, s);

        PrinterConfig.ClassCfg c = PrinterConfig.byKey(cls);
        if (named) TextUtil.msg(player, "&3Tu as posé un Printer " + c.color + cls + " &3!");
        else TextUtil.msg(player, "&3Tu as posé un Printer (Silver par défaut).");
    }

    // ------------------------------------------------------------------ casse

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getPlayer() instanceof ServerPlayer player)) return;
        if (!PrinterConfig.isPrinterBlock(e.getState())) return;

        BlockPos pos = e.getPos();
        PrinterData data = PrinterData.get(level);
        PrinterData.State s = data.get(pos);

        // 1. Si le joueur est le propriétaire, la casse est instantanée
        if (s != null && player.getUUID().equals(s.owner)) {
            data.remove(pos);
            TextUtil.msg(player, PrinterConfig.MSG_BREAK.get());
            return;
        }

        // 2. Ce n'est pas le propriétaire : on Annule la casse immédiate de Minecraft
        e.setCanceled(true);

        // Récupération du temps en secondes configuré (ex: 20s)
        long secondsToBreak = TIME_TO_BREAK.get().longValue();
        long millisToBreak = secondsToBreak * 1000L; // Conversion en millisecondes pour Thread.sleep

        // 3. Alerte les joueurs situés dans un rayon de X blocs autour du printer
        double radius = BREAK_RADIUS.get().doubleValue();
        double radiusSq = radius * radius;
        Vec3 center = pos.getCenter();

        for (ServerPlayer nearPlayer : level.players()) {
            if (nearPlayer.distanceToSqr(center) <= radiusSq) {
                nearPlayer.sendSystemMessage(
                        Component.literal("⚠ " + player.getScoreboardName() + " tente de détruire un printer près de vous ! Destruction dans " + secondsToBreak + "s.")
                );
            }
        }

        // 4. Lancement de la tâche différée en arrière-plan
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(millisToBreak);
            } catch (InterruptedException ignored) {}
        }).thenAcceptAsync(v -> {
            // Exécution sur le thread principal du serveur
            level.getServer().execute(() -> {
                // Vérifie que le bloc est toujours présent avant de le détruire
                if (PrinterConfig.isPrinterBlock(level.getBlockState(pos))) {
                    PrinterData.get(level).remove(pos);
                    // destroyBlock simule la casse avec particules et drops (ou false si pas de drops)
                    level.destroyBlock(pos, true);
                }
            });
        });
    }

    // ------------------------------------------------------------------ clic droit

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) e.getLevel();
        ItemStack held = e.getItemStack();

        // Mémorise la classe de l'item Printer tenu (utilisée à la pose)
        if (held.getItem() instanceof BlockItem bi && PrinterConfig.isPrinterBlock(bi.getBlock().defaultBlockState())) {
            String cls = PrinterConfig.classFromName(held);
            PENDING_CLASS.put(player.getUUID(), cls == null ? "" : cls);
        } else {
            PENDING_CLASS.remove(player.getUUID());
        }

        BlockPos pos = e.getPos();
        if (!PrinterConfig.isPrinterBlock(level.getBlockState(pos))) return;
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        // Shift + objet en main = comportement vanilla (poser un bloc à côté)
        if (player.isShiftKeyDown() && !held.isEmpty()) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        handleUse(level, player, pos, held);
    }

    private static void handleUse(ServerLevel level, ServerPlayer p, BlockPos pos, ItemStack held) {
        PrinterData data = PrinterData.get(level);
        PrinterData.State s = data.getOrCreate(pos);
        data.setDirty();

        // --- Upgrade de capacité ---
        if (PrinterConfig.matches(held, PrinterConfig.CAPACITY_ITEM)) {
            int limit = PrinterConfig.byKey(s.cls).capLimit.get();
            if (s.powerMax >= limit) {
                TextUtil.msg(p, "&cCe Printer a déjà atteint la capacité de batterie maximale (" + limit + "%) !");
                return;
            }
            s.powerMax = Math.min(limit, s.powerMax + PrinterConfig.CAPACITY_STEP.get());
            held.shrink(1);
            TextUtil.msg(p, "&aCapacité de batterie augmentée ! Nouvelle capacité max : &e" + s.powerMax + "% &7(Max " + limit + "%)");
            sound(p, "block.anvil.use", 1.5f);
            return;
        }

        // --- Upgrade de vitesse ---
        if (PrinterConfig.matches(held, PrinterConfig.SPEED_ITEM)) {
            int max = PrinterConfig.MAX_SPEED.get();
            if (s.speed >= max) {
                TextUtil.msg(p, "&cCe Printer a déjà atteint la vitesse maximum (" + max + "x) !");
                return;
            }
            s.speed = Math.min(max, s.speed + 1);
            held.shrink(1);
            TextUtil.msg(p, "&aVitesse augmentée ! Nouvelle vitesse : &e" + TextUtil.num(s.speed) + "x &7(Max " + max + "x)");
            sound(p, "block.brewing_stand.brew", 1.5f);
            return;
        }

        // --- Encre : N clics puis lancement automatique ---
        if (PrinterConfig.matches(held, PrinterConfig.INK_ITEM)) {
            if (s.running) {
                TextUtil.msg(p, "Le printer est déjà en cours ou en attente, patiente !");
                return;
            }
            if (s.power <= 0) {
                TextUtil.msg(p, "Tu n'as plus de Batterie !");
                return;
            }
            int needed = PrinterConfig.INK_NEEDED.get();
            held.shrink(1);
            s.ink++;
            TextUtil.msg(p, "&3Encre chargée : " + s.ink + "/" + needed);
            if (s.ink >= needed) {
                TextUtil.msg(p, "&aEncre complète ! Le printer démarre...");
                sound(p, "block.note_block.pling", 1.5f);
                s.ink = 0;
                s.running = true;
                s.progress = 0;
                s.acc = 0;
                s.stage = 0;
                s.runner = p.getUUID();
            }
            return;
        }

        // --- Recharge par batterie ---
        int amount = PrinterConfig.batteryAmount(held);
        if (amount >= 0) {
            if (s.power >= s.powerMax) {
                TextUtil.msg(p, "&cLa batterie est déjà pleine (Max actuel: " + s.powerMax + "%) !");
                return;
            }
            s.power = Math.min(s.powerMax, s.power + amount);
            held.shrink(1);
            TextUtil.msg(p, "Tu viens de rajouter de la batterie au Printer (Actuel: " + s.power + "% / " + s.powerMax + "%)");
            return;
        }

        // --- Sinon : informations ---
        sendInfo(p, pos, s);
    }

    // ------------------------------------------------------------------ impression (tick)

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            PrinterData data = PrinterData.get(level);
            Iterator<Map.Entry<Long, PrinterData.State>> it = data.all().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, PrinterData.State> en = it.next();
                PrinterData.State s = en.getValue();
                if (!s.running) continue;
                BlockPos pos = BlockPos.of(en.getKey());
                if (!level.hasChunkAt(pos)) continue; // chunk déchargé : pause
                if (!PrinterConfig.isPrinterBlock(level.getBlockState(pos))) {
                    it.remove(); // bloc disparu (explosion, piston...)
                    data.setDirty();
                    continue;
                }
                tickPrinter(level, data, pos, s);
            }
        }
    }

    private static void tickPrinter(ServerLevel level, PrinterData data, BlockPos pos, PrinterData.State s) {
        ServerPlayer runner = s.runner == null ? null : level.getServer().getPlayerList().getPlayer(s.runner);
        double speed = s.speed <= 0 ? 1.0 : s.speed;
        double delay = 20.0 / speed; // ticks entre deux étapes
        int total = PrinterConfig.PRINT_STEPS.get();
        int perStage = Math.max(1, total / 10);

        s.acc += 1.0;
        data.setDirty();

        while (s.running && s.acc >= delay) {
            s.acc -= delay;

            if (s.power <= 0) {
                if (runner != null) TextUtil.msg(runner, "Tu n'as plus de Batterie ! Impression interrompue à " + s.stage + "%.");
                resetRun(s);
                return;
            }

            s.progress++;
            sound(level, pos, "block.note_block.hat", 1.0f, 1.2f);

            if (s.progress % perStage == 0) {
                s.power = Math.max(0, s.power - PrinterConfig.POWER_PER_STAGE.get());
                s.stage = Math.min(100, s.progress * 100 / total);
                if (runner != null && runner.level() == level && runner.distanceToSqr(Vec3.atCenterOf(pos)) <= 100.0) {
                    TextUtil.msg(runner, "création des Billets à " + s.stage + "%");
                }
            }

            if (s.progress >= total) {
                finish(level, pos, s, runner);
                return;
            }
        }
    }

    private static void finish(ServerLevel level, BlockPos pos, PrinterData.State s, ServerPlayer runner) {
        PrinterConfig.ClassCfg c = PrinterConfig.byKey(s.cls);
        int min = c.dropMin.get();
        int max = Math.max(min, c.dropMax.get());
        int amount = min + level.random.nextInt(max - min + 1);

        Item bill = PrinterConfig.item(PrinterConfig.BILL_ITEM.get());
        if (bill == Items.AIR) {
            PrinterMod.LOGGER.error("[Printer] billItem '{}' introuvable, aucun billet donné.", PrinterConfig.BILL_ITEM.get());
        } else if (amount > 0) {
            ItemStack stack = new ItemStack(bill, amount);
            if (runner != null) ItemHandlerHelper.giveItemToPlayer(runner, stack);
            else Block.popResource(level, pos.above(), stack);
        }

        if (runner != null) {
            sound(runner, "entity.player.levelup", 1.0f);
            TextUtil.msg(runner, "Création terminée !");
            TextUtil.msg(runner, "&eTu as reçu " + amount + " billets ! (&7Classe : " + s.cls + "&e)");
        }
        resetRun(s);
    }

    private static void resetRun(PrinterData.State s) {
        s.running = false;
        s.progress = 0;
        s.acc = 0;
        s.stage = 0;
        s.ink = 0;
        s.runner = null;
    }
}
