package com.moneymantra.emfblockdrop.item;

import com.moneymantra.emfblockdrop.capture.BlockCaptureManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class BlockCaptureCardItem extends Item {

  public BlockCaptureCardItem() {
    super(new Item.Properties().stacksTo(1));
  }

  @Override
  public Component getName(ItemStack stack) {
    if (BlockCaptureManager.isValidCard(stack)) {
      return BlockCaptureManager.buildCardName(stack);
    }
    return super.getName(stack);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      TooltipContext tooltipContext,
      List<Component> tooltip,
      TooltipFlag flag) {
    super.appendHoverText(stack, tooltipContext, tooltip, flag);
    if (BlockCaptureManager.isValidCard(stack)) {
      tooltip.add(Component.literal("Captured Block: ").withStyle(ChatFormatting.GRAY).append(BlockCaptureManager.getCapturedBlockName(stack).copy().withStyle(ChatFormatting.WHITE)));
      tooltip.add(BlockCaptureManager.getCapturedBlockId(stack));
    } else {
      tooltip.add(Component.literal("Empty / Invalid capture data").withStyle(ChatFormatting.RED));
    }
  }
}
