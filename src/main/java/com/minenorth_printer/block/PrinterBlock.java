package com.minenorth_printer.block;

import com.minenorth_printer.ModRegistry;
import com.minenorth_printer.PrinterConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class PrinterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Hitbox du modèle 3D (corps + capot + bac papier), identique dans les 4 orientations. */
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    /** Avantage de grade (mod VIP) qui donne accès à l'imprimante dorée. */
    public static final String GOLD_PERK = "imprimanteOr";

    public final boolean gold;

    public PrinterBlock(Properties props) {
        this(props, false);
    }

    public PrinterBlock(Properties props, boolean gold) {
        super(props);
        this.gold = gold;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    /** Le joueur a-t-il le droit d'utiliser une imprimante dorée (grade VIP) ? */
    public static boolean goldAllowed(net.minecraft.server.MinecraftServer server, java.util.UUID player) {
        return fr.minenorth.api.MineNorth.grades().perk(server, player, GOLD_PERK, 0) > 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (gold && ctx.getPlayer() instanceof ServerPlayer sp && !goldAllowed(sp.server, sp.getUUID())) {
            sp.displayClientMessage(Component.literal("§cImprimante dorée : réservée aux VIP."), true);
            return null;
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrinterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModRegistry.PRINTER_BE.get(),
                level.isClientSide ? PrinterBlockEntity::clientTick : PrinterBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player p && level.getBlockEntity(pos) instanceof PrinterBlockEntity be) {
            be.setOwner(p);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof PrinterBlockEntity be) {
            if (gold && !goldAllowed(sp.server, sp.getUUID()) && !sp.hasPermissions(2)) {
                sp.displayClientMessage(Component.literal("§cImprimante dorée : réservée aux VIP."), true);
                return InteractionResult.CONSUME;
            }
            if (PrinterConfig.get(PrinterConfig.ONLY_OWNER_CAN_OPEN) && !be.isOwner(sp) && !sp.hasPermissions(2)) {
                sp.displayClientMessage(Component.translatable("message.minenorth_printer.not_owner"), true);
                return InteractionResult.CONSUME;
            }
            NetworkHooks.openScreen(sp, be, buf -> {
                buf.writeBlockPos(pos);
                buf.writeUtf(be.getOwnerName());
            });
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof PrinterBlockEntity be) be.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
