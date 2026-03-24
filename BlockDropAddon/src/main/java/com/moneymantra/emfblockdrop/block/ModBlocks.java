package com.moneymantra.emfblockdrop.block;

import com.moneymantra.emfblockdrop.Constants;
import com.moneymantra.emfblockdrop.block.entity.BlockDropFarmBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
      DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

  public static final DeferredBlock<Block> BLOCK_DROP_FARM =
      BLOCKS.register("block_drop_farm", BlockDropFarmBlock::new);

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockDropFarmBlockEntity>> BLOCK_DROP_FARM_ENTITY =
      BLOCK_ENTITY_TYPES.register(
          BlockDropFarmBlockEntity.ID,
          () -> BlockEntityType.Builder.of(BlockDropFarmBlockEntity::new, BLOCK_DROP_FARM.get()).build(null));

  private ModBlocks() {}
}
