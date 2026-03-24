package com.moneymantra.emfblockdrop.item;

import de.markusbordihn.easymobfarm.block.entity.MobFarmBlockEntity;
import de.markusbordihn.easymobfarm.component.DataComponents;
import de.markusbordihn.easymobfarm.data.mobfarm.MobFarmData;
import de.markusbordihn.easymobfarm.data.mobfarm.MobFarmTierLevel;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BlockDropFarmBlockItem extends BlockItem {

  public BlockDropFarmBlockItem(Block block, Item.Properties properties) {
    super(block, properties);
  }

  public static MobFarmTierLevel getTierLevel(ItemStack stack) {
    MobFarmData mobFarmData = stack.getOrDefault(DataComponents.MOB_FARM_DATA, MobFarmData.EMPTY);
    return mobFarmData.tierLevel();
  }

  public static void updateCustomModelData(ItemStack stack) {
    int tierLevel = getTierLevel(stack).getTierLevel();
    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(tierLevel));
  }

  @Override
  public Component getName(ItemStack stack) {
    return Component.translatable("block.easy_mob_farm_block_drop_addon.block_drop_farm");
  }

  @Override
  public void onCraftedBy(ItemStack stack, Level level, Player player) {
    super.onCraftedBy(stack, level, player);
    updateCustomModelData(stack);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      TooltipContext tooltipContext,
      List<Component> tooltip,
      TooltipFlag flag) {
    super.appendHoverText(stack, tooltipContext, tooltip, flag);
    int tierLevel = getTierLevel(stack).getTierLevel();
    tooltip.add(Component.translatable("tooltip.easy_mob_farm_block_drop_addon.block_drop_farm.desc").withStyle(ChatFormatting.GRAY));
    tooltip.add(Component.translatable("text.easy_mob_farm.tier_level", String.valueOf(tierLevel)).withStyle(switch (tierLevel) {
      case 1 -> ChatFormatting.GREEN;
      case 2 -> ChatFormatting.YELLOW;
      case 3 -> ChatFormatting.RED;
      default -> ChatFormatting.WHITE;
    }));
    tooltip.add(Component.translatable("tooltip.easy_mob_farm_block_drop_addon.processing_speed", MobFarmBlockEntity.getProcessingSpeed(tierLevel)).withStyle(ChatFormatting.GRAY));
  }
}
