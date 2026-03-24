package com.moneymantra.emfblockdrop.block.entity;

import com.moneymantra.emfblockdrop.block.BlockDropFarmBlock;
import com.moneymantra.emfblockdrop.block.ModBlocks;
import com.moneymantra.emfblockdrop.capture.BlockCaptureManager;
import de.markusbordihn.easymobfarm.component.DataComponents;
import de.markusbordihn.easymobfarm.config.MobFarmConfig;
import de.markusbordihn.easymobfarm.data.mobfarm.MobFarmData;
import de.markusbordihn.easymobfarm.data.mobfarm.MobFarmTierLevel;
import de.markusbordihn.easymobfarm.item.upgrade.SlotUpgradeItem;
import de.markusbordihn.easymobfarm.item.upgrade.enhancement.SpeedEnhancementItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDropFarmBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

  public static final String ID = "block_drop_farm";
  public static final int CONTAINER_SIZE = 36;
  public static final int CARD_SLOT = 0;
  public static final int SPEED_SLOT_START = 1;
  public static final int SPEED_SLOT_END = 4;
  public static final int SLOT_UPGRADE_START = 5;
  public static final int SLOT_UPGRADE_END = 7;
  public static final int OUTPUT_START = 9;
  public static final int OUTPUT_END = 35;
  public static final int[] OUTPUT_SLOTS = java.util.stream.IntStream.rangeClosed(OUTPUT_START, OUTPUT_END).toArray();
  public static final int[] NO_SLOTS = new int[0];
  public static final int DEFAULT_PROCESSING_TICKS = 20;
  public static final int DEFAULT_RECHECK_TICKS = 200;
  public static final int MIN_OUTPUT_SLOTS = 6;
  public static final int MAX_OUTPUT_SLOTS = 27;

  private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
  private final int processingDelay;
  private int farmTierLevel = 0;
  private int farmProgress = 0;
  private int numberOfOutputSlots = MIN_OUTPUT_SLOTS;
  private UUID owner;

  public BlockDropFarmBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlocks.BLOCK_DROP_FARM_ENTITY.get(), pos, state);
    this.processingDelay =
        Math.min(
            Math.max((Math.abs(pos.getX() * 31 + pos.getZ() * 17) % DEFAULT_PROCESSING_TICKS), 0),
            DEFAULT_PROCESSING_TICKS - 1);
  }

  public static void serverTick(Level level, BlockPos pos, BlockState state, BlockDropFarmBlockEntity farm) {
    if (level.getGameTime() % DEFAULT_RECHECK_TICKS == 0) {
      farm.updateNumberOfOutputSlots();
    }

    if (state.getValue(BlockDropFarmBlock.POWERED)) {
      farm.setWorking(false);
      return;
    }

    if (!farm.hasValidCard()) {
      farm.farmProgress = 0;
      farm.setWorking(false);
      return;
    }

    if (!farm.canProcessResults()) {
      farm.setWorking(false);
      return;
    }

    if (level.getGameTime() % DEFAULT_PROCESSING_TICKS != farm.processingDelay) {
      return;
    }

    if (farm.farmProgress < MobFarmConfig.farmProgressingTime) {
      farm.farmProgress = Math.min(farm.farmProgress + farm.getEffectiveFarmProgressionSpeed(), MobFarmConfig.farmProgressingTime);
      farm.setWorking(true);
      farm.setChanged();
      return;
    }

    if (MobFarmConfig.processingRequiresOwnerToBeOnline && farm.owner != null && level.getPlayerByUUID(farm.owner) == null) {
      farm.setWorking(false);
      return;
    }

    ItemStack output = BlockCaptureManager.createFarmOutput(farm.items.get(CARD_SLOT));
    if (output.isEmpty()) {
      farm.farmProgress = 0;
      farm.setWorking(false);
      return;
    }

    if (farm.insertOutput(output)) {
      farm.farmProgress = 0;
      farm.setWorking(true);
      farm.setChanged();
    }
  }

  public Component getDisplayName() {
    return Component.translatable("block.easy_mob_farm_block_drop_addon.block_drop_farm");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
    return ChestMenu.fourRows(id, playerInventory, this);
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
  }

  public int getFarmTierLevel() {
    return farmTierLevel;
  }

  public void setFarmTierLevel(int farmTierLevel) {
    this.farmTierLevel = Math.max(0, Math.min(3, farmTierLevel));
    this.numberOfOutputSlots = calculateNumberOfOutputSlots();
    if (level != null && !level.isClientSide) {
      BlockState state = getBlockState();
      if (state.getValue(BlockDropFarmBlock.TIER_LEVEL) != this.farmTierLevel) {
        level.setBlock(worldPosition, state.setValue(BlockDropFarmBlock.TIER_LEVEL, this.farmTierLevel), 3);
      }
    }
    setChanged();
  }

  public boolean tryInsertHeldItem(Player player, net.minecraft.world.InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (stack.isEmpty()) {
      return false;
    }
    for (int slot = 0; slot < 8; slot++) {
      if (canPlaceItem(slot, stack) && items.get(slot).isEmpty()) {
        ItemStack copy = stack.copyWithCount(1);
        items.set(slot, copy);
        if (!player.getAbilities().instabuild) {
          stack.shrink(1);
        }
        updateNumberOfOutputSlots();
        setChanged();
        return true;
      }
    }
    return false;
  }

  public boolean tryExtractCard(Player player, net.minecraft.world.InteractionHand hand) {
    ItemStack stack = items.get(CARD_SLOT);
    if (stack.isEmpty() || !player.getItemInHand(hand).isEmpty()) {
      return false;
    }
    player.setItemInHand(hand, stack.copy());
    items.set(CARD_SLOT, ItemStack.EMPTY);
    farmProgress = 0;
    setChanged();
    return true;
  }

  public boolean hasValidCard() {
    return BlockCaptureManager.isValidCard(items.get(CARD_SLOT));
  }

  public List<ItemStack> getDroppedInventoryView() {
    List<ItemStack> drops = new ArrayList<>();
    for (ItemStack item : items) {
      if (!item.isEmpty()) {
        drops.add(item.copy());
      }
    }
    return drops;
  }

  private int getEffectiveFarmProgressionSpeed() {
    int speed = DEFAULT_PROCESSING_TICKS + MobFarmConfig.getFarmTierProgressionUpgradeSpeed(farmTierLevel);
    for (int slot = SPEED_SLOT_START; slot <= SPEED_SLOT_END; slot++) {
      ItemStack itemStack = items.get(slot);
      if (!itemStack.isEmpty() && itemStack.getItem() instanceof SpeedEnhancementItem speedEnhancementItem) {
        speed += speedEnhancementItem.getUpgradeSpeed();
      }
    }
    return speed;
  }

  private int calculateNumberOfOutputSlots() {
    int slots = MIN_OUTPUT_SLOTS;
    for (int slot = SLOT_UPGRADE_START; slot <= SLOT_UPGRADE_END; slot++) {
      ItemStack itemStack = items.get(slot);
      if (!itemStack.isEmpty() && itemStack.getItem() instanceof SlotUpgradeItem slotUpgradeItem) {
        slots += slotUpgradeItem.numberOfUpgradeSlots();
      }
    }
    return Math.min(MAX_OUTPUT_SLOTS, Math.max(MIN_OUTPUT_SLOTS, slots));
  }

  public void updateNumberOfOutputSlots() {
    numberOfOutputSlots = calculateNumberOfOutputSlots();
  }

  public boolean canProcessResults() {
    int maxExclusive = OUTPUT_START + numberOfOutputSlots;
    ItemStack output = BlockCaptureManager.createFarmOutput(items.get(CARD_SLOT));
    if (output.isEmpty()) {
      return false;
    }
    for (int slot = OUTPUT_START; slot < maxExclusive; slot++) {
      ItemStack existing = items.get(slot);
      if (existing.isEmpty()) {
        return true;
      }
      if (ItemStack.isSameItemSameComponents(existing, output) && existing.getCount() < existing.getMaxStackSize()) {
        return true;
      }
    }
    return false;
  }

  private boolean insertOutput(ItemStack stack) {
    int maxExclusive = OUTPUT_START + numberOfOutputSlots;
    for (int slot = OUTPUT_START; slot < maxExclusive; slot++) {
      ItemStack existing = items.get(slot);
      if (existing.isEmpty()) {
        items.set(slot, stack.copy());
        return true;
      }
      if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
        existing.grow(1);
        return true;
      }
    }
    return false;
  }

  private void setWorking(boolean working) {
    if (level != null && !level.isClientSide) {
      BlockState state = getBlockState();
      if (state.getValue(BlockDropFarmBlock.WORKING) != working) {
        level.setBlock(worldPosition, state.setValue(BlockDropFarmBlock.WORKING, working), 3);
      }
    }
  }

  @Override
  public int[] getSlotsForFace(Direction direction) {
    return direction == Direction.DOWN ? OUTPUT_SLOTS : NO_SLOTS;
  }

  @Override
  public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
    return false;
  }

  @Override
  public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
    return index >= OUTPUT_START && index < OUTPUT_START + numberOfOutputSlots;
  }

  @Override
  public int getContainerSize() {
    return CONTAINER_SIZE;
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack item : items) {
      if (!item.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public ItemStack getItem(int index) {
    return items.get(index);
  }

  @Override
  public ItemStack removeItem(int index, int count) {
    ItemStack stack = ContainerHelper.removeItem(items, index, count);
    if (!stack.isEmpty()) {
      if (index >= SLOT_UPGRADE_START && index <= SLOT_UPGRADE_END) {
        updateNumberOfOutputSlots();
      }
      setChanged();
    }
    return stack;
  }

  @Override
  public ItemStack removeItemNoUpdate(int index) {
    ItemStack stack = items.get(index);
    items.set(index, ItemStack.EMPTY);
    if (index >= SLOT_UPGRADE_START && index <= SLOT_UPGRADE_END) {
      updateNumberOfOutputSlots();
    }
    return stack;
  }

  @Override
  public void setItem(int index, ItemStack stack) {
    items.set(index, stack);
    if (index == CARD_SLOT && !stack.isEmpty()) {
      farmProgress = 0;
    }
    if (index >= SLOT_UPGRADE_START && index <= SLOT_UPGRADE_END) {
      updateNumberOfOutputSlots();
    }
    setChanged();
  }

  @Override
  public boolean stillValid(Player player) {
    if (level == null || level.getBlockEntity(worldPosition) != this) {
      return false;
    }
    return player.distanceToSqr(
            worldPosition.getX() + 0.5D,
            worldPosition.getY() + 0.5D,
            worldPosition.getZ() + 0.5D)
        <= 64.0D;
  }

  @Override
  public boolean canPlaceItem(int index, ItemStack stack) {
    if (index == CARD_SLOT) {
      return BlockCaptureManager.isValidCard(stack);
    }
    if (index >= SPEED_SLOT_START && index <= SPEED_SLOT_END) {
      return !stack.isEmpty() && stack.getItem() instanceof SpeedEnhancementItem;
    }
    if (index >= SLOT_UPGRADE_START && index <= SLOT_UPGRADE_END) {
      return !stack.isEmpty() && stack.getItem() instanceof SlotUpgradeItem;
    }
    return false;
  }

  @Override
  public void clearContent() {
    for (int i = 0; i < items.size(); i++) {
      items.set(i, ItemStack.EMPTY);
    }
    farmProgress = 0;
    setChanged();
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    ContainerHelper.loadAllItems(tag, items, registries);
    farmTierLevel = tag.getInt("FarmTierLevel");
    farmProgress = tag.getInt("FarmProgress");
    numberOfOutputSlots = tag.getInt("NumberOfOutputSlots");
    if (tag.hasUUID("Owner")) {
      owner = tag.getUUID("Owner");
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    ContainerHelper.saveAllItems(tag, items, registries);
    tag.putInt("FarmTierLevel", farmTierLevel);
    tag.putInt("FarmProgress", farmProgress);
    tag.putInt("NumberOfOutputSlots", numberOfOutputSlots);
    if (owner != null) {
      tag.putUUID("Owner", owner);
    }
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return saveWithoutMetadata(registries);
  }
}
