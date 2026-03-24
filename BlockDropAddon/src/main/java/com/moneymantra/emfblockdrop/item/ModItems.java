package com.moneymantra.emfblockdrop.item;

import com.moneymantra.emfblockdrop.Constants;
import com.moneymantra.emfblockdrop.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

  public static final DeferredItem<Item> BLOCK_CAPTURE_CARD =
      ITEMS.register("block_capture_card", BlockCaptureCardItem::new);

  public static final DeferredHolder<Item, BlockItem> BLOCK_DROP_FARM =
      ITEMS.register(
          "block_drop_farm",
          () -> new BlockDropFarmBlockItem(ModBlocks.BLOCK_DROP_FARM.get(), new Item.Properties()));

  private ModItems() {}
}
