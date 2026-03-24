package com.moneymantra.emfblockdrop;

import com.moneymantra.emfblockdrop.block.ModBlocks;
import com.moneymantra.emfblockdrop.component.ModDataComponents;
import com.moneymantra.emfblockdrop.config.BlockDropFarmConfig;
import com.moneymantra.emfblockdrop.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Constants.MOD_ID)
public class EasyMobFarmBlockDropAddon {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  public EasyMobFarmBlockDropAddon(IEventBus modEventBus) {
    log.info("Initializing {} ...", Constants.MOD_NAME);

    BlockDropFarmConfig.registerConfig();

    ModBlocks.BLOCKS.register(modEventBus);
    ModBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
    ModItems.ITEMS.register(modEventBus);
    ModDataComponents.DATA_COMPONENTS.register(modEventBus);

    modEventBus.addListener(this::buildCreativeTabs);
  }

  private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
      event.accept(ModItems.BLOCK_DROP_FARM);
    }
    if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
      event.accept(ModItems.BLOCK_CAPTURE_CARD);
    }
  }
}
