package com.moneymantra.emfblockdrop.capture;

import com.moneymantra.emfblockdrop.Constants;
import com.moneymantra.emfblockdrop.component.ModDataComponents;
import com.moneymantra.emfblockdrop.config.BlockDropFarmConfig;
import com.moneymantra.emfblockdrop.data.BlockCaptureData;
import com.moneymantra.emfblockdrop.item.ModItems;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockCaptureManager {

  public static final TagKey<Block> BLACKLIST_TAG =
      TagKey.create(
          net.minecraft.core.registries.Registries.BLOCK,
          ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block_capture_card_blacklist"));
  public static final TagKey<Block> WHITELIST_TAG =
      TagKey.create(
          net.minecraft.core.registries.Registries.BLOCK,
          ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block_capture_card_whitelist"));

  private BlockCaptureManager() {}

  public static boolean canRollCaptureCard(ServerPlayer player) {
    return BlockDropFarmConfig.allowFakePlayers || !(player instanceof net.neoforged.neoforge.common.util.FakePlayer);
  }

  public static boolean isEligibleBlock(BlockState blockState) {
    if (blockState.is(WHITELIST_TAG)) {
      return true;
    }
    if (blockState.isAir() || blockState.getFluidState().isSource() || blockState.is(BLACKLIST_TAG)) {
      return false;
    }

    Block block = blockState.getBlock();
    Item item = block.asItem();
    if (!(item instanceof BlockItem)) {
      return false;
    }

    ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
    String path = key.getPath().toLowerCase(Locale.ROOT);

    if (path.contains("air")
        || path.contains("portal")
        || path.contains("fire")
        || path.contains("command_block")
        || path.contains("structure_block")
        || path.contains("structure_void")
        || path.contains("jigsaw")
        || path.contains("barrier")
        || path.contains("debug")
        || path.contains("light")
        || path.contains("moving_piston")
        || path.contains("end_gateway")
        || path.contains("end_portal")
        || path.contains("nether_portal")
        || path.contains("bedrock")
        || path.contains("spawner")
        || path.contains("trial_spawner")
        || path.contains("vault")
        || path.contains("reinforced_deepslate")) {
      return false;
    }

    if (BlockDropFarmConfig.excludeOresByName
        && (path.contains("_ore") || path.startsWith("ore_") || path.endsWith("_ore") || path.contains("ore_block"))) {
      return false;
    }

    if (BlockDropFarmConfig.excludeBlockEntities
        && (block instanceof EntityBlock || block instanceof BaseEntityBlock)) {
      return false;
    }

    return true;
  }

  public static boolean droppedSelf(BlockState state, List<ItemStack> drops) {
    Item item = state.getBlock().asItem();
    if (!(item instanceof BlockItem)) {
      return false;
    }
    for (ItemStack drop : drops) {
      if (!drop.isEmpty() && drop.getItem() == item) {
        return true;
      }
    }
    return false;
  }

  public static ItemStack createCard(Block block) {
    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
    ItemStack stack = new ItemStack(ModItems.BLOCK_CAPTURE_CARD.get());
    stack.set(ModDataComponents.BLOCK_CAPTURE_DATA.get(), new BlockCaptureData(blockId));
    stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0));
    return stack;
  }

  public static Block getCapturedBlock(ItemStack stack) {
    BlockCaptureData data = stack.get(ModDataComponents.BLOCK_CAPTURE_DATA.get());
    if (data == null) {
      return null;
    }
    return BuiltInRegistries.BLOCK.getOptional(data.blockId()).orElse(null);
  }

  public static boolean isValidCard(ItemStack stack) {
    if (!stack.is(ModItems.BLOCK_CAPTURE_CARD.get())) {
      return false;
    }
    Block block = getCapturedBlock(stack);
    return block != null && block != net.minecraft.world.level.block.Blocks.AIR;
  }

  public static Component getCapturedBlockName(ItemStack stack) {
    Block block = getCapturedBlock(stack);
    return block == null ? Component.literal("Unknown Block") : block.getName();
  }

  public static Component getCapturedBlockId(ItemStack stack) {
    BlockCaptureData data = stack.get(ModDataComponents.BLOCK_CAPTURE_DATA.get());
    return data == null
        ? Component.literal("unknown")
        : Component.literal(data.blockId().toString()).withStyle(ChatFormatting.DARK_GRAY);
  }

  public static ItemStack createFarmOutput(ItemStack cardStack) {
    Block block = getCapturedBlock(cardStack);
    if (block == null) {
      return ItemStack.EMPTY;
    }
    Item item = block.asItem();
    if (!(item instanceof BlockItem)) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(item);
  }

  public static void spawnCard(ServerLevel level, BlockPos pos, ItemStack card) {
    ItemEntity itemEntity =
        new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, card);
    itemEntity.setDefaultPickUpDelay();
    level.addFreshEntity(itemEntity);
  }

  public static MutableComponent buildCardName(ItemStack stack) {
    return getCapturedBlockName(stack).copy().append(Component.literal(" Capture Card"));
  }
}
