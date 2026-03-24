package com.moneymantra.emfblockdrop.block;

import com.moneymantra.emfblockdrop.block.entity.BlockDropFarmBlockEntity;
import com.moneymantra.emfblockdrop.item.BlockDropFarmBlockItem;
import de.markusbordihn.easymobfarm.component.DataComponents;
import de.markusbordihn.easymobfarm.data.mobfarm.MobFarmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BlockDropFarmBlock extends BaseEntityBlock {

  public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
  public static final IntegerProperty TIER_LEVEL = IntegerProperty.create("tier_level", 0, 3);
  public static final BooleanProperty WORKING = BooleanProperty.create("working");
  public static final BooleanProperty POWERED = BooleanProperty.create("powered");

  public BlockDropFarmBlock() {
    super(
        Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(5.0f)
            .sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(WORKING) ? 15 : 8)
            .noOcclusion());
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(TIER_LEVEL, 0)
            .setValue(WORKING, false)
            .setValue(POWERED, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, TIER_LEVEL, WORKING, POWERED);
  }

  @Override
  public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  public void setPlacedBy(
      Level level,
      BlockPos pos,
      BlockState state,
      @Nullable LivingEntity placer,
      ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (blockEntity instanceof BlockDropFarmBlockEntity farm) {
      MobFarmData data = stack.getOrDefault(DataComponents.MOB_FARM_DATA, MobFarmData.EMPTY);
      int tierLevel = data.tierLevel().getTierLevel();
      farm.setFarmTierLevel(tierLevel);
      if (placer instanceof ServerPlayer serverPlayer) {
        farm.setOwner(serverPlayer.getUUID());
      }
      if (level instanceof ServerLevel serverLevel) {
        BlockState newState = state.setValue(TIER_LEVEL, tierLevel).setValue(POWERED, level.hasNeighborSignal(pos));
        serverLevel.setBlock(pos, newState, Block.UPDATE_ALL);
        farm.setChanged();
      }
    }
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
    if (!level.isClientSide && level.getBlockEntity(pos) instanceof BlockDropFarmBlockEntity farm) {
      player.openMenu(farm);
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  public ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hitResult) {
    if (level.isClientSide) {
      return ItemInteractionResult.SUCCESS;
    }
    if (!(level.getBlockEntity(pos) instanceof BlockDropFarmBlockEntity farm)) {
      return ItemInteractionResult.FAIL;
    }
    if (!stack.isEmpty() && farm.tryInsertHeldItem(player, hand)) {
      return ItemInteractionResult.CONSUME;
    }
    if (player.isShiftKeyDown() && farm.tryExtractCard(player, hand)) {
      return ItemInteractionResult.CONSUME;
    }
    player.openMenu(farm);
    return ItemInteractionResult.CONSUME;
  }

  @Override
  public void neighborChanged(
      BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
    if (!level.isClientSide) {
      boolean powered = level.hasNeighborSignal(pos);
      if (state.getValue(POWERED) != powered) {
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
      }
    }
  }

  @Override
  public void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      if (level.getBlockEntity(pos) instanceof BlockDropFarmBlockEntity farm) {
        for (ItemStack itemStack : farm.getDroppedInventoryView()) {
          Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
        }
      }
      super.onRemove(state, level, pos, newState, isMoving);
    }
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new BlockDropFarmBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide
        ? null
        : createTickerHelper(type, ModBlocks.BLOCK_DROP_FARM_ENTITY.get(), BlockDropFarmBlockEntity::serverTick);
  }

  @Override
  protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    ItemStack drop = new ItemStack(asItem());
    int tierLevel = state.getValue(TIER_LEVEL);
    if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockDropFarmBlockEntity farm) {
      tierLevel = farm.getFarmTierLevel();
    }
    drop.set(
        DataComponents.MOB_FARM_DATA,
        new MobFarmData(
            de.markusbordihn.easymobfarm.data.mobfarm.MobFarmTierLevel.getTierLevel(tierLevel)));
    BlockDropFarmBlockItem.updateCustomModelData(drop);
    return java.util.List.of(drop);
  }
}
