package com.moneymantra.emfblockdrop.event;

import com.moneymantra.emfblockdrop.capture.BlockCaptureManager;
import com.moneymantra.emfblockdrop.config.BlockDropFarmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public final class BlockBreakEvents {

  private BlockBreakEvents() {}

  @SubscribeEvent
  public static void onBlockBreak(BlockEvent.BreakEvent event) {
    if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)
        || !(event.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }
    if (serverPlayer.isCreative() || !BlockCaptureManager.canRollCaptureCard(serverPlayer)) {
      return;
    }
    if (serverPlayer.getRandom().nextInt(BlockDropFarmConfig.captureCardChance) != 0) {
      return;
    }

    BlockPos pos = event.getPos();
    BlockState state = serverLevel.getBlockState(pos);
    if (!BlockCaptureManager.isEligibleBlock(state)) {
      return;
    }

    BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
    ItemStack tool = serverPlayer.getMainHandItem();
    java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, serverLevel, pos, blockEntity, serverPlayer, tool);
    if (BlockDropFarmConfig.dropCardRequiresSelfDrop && !BlockCaptureManager.droppedSelf(state, drops)) {
      return;
    }

    ItemStack card = BlockCaptureManager.createCard(state.getBlock());
    BlockCaptureManager.spawnCard(serverLevel, pos, card);
  }
}
