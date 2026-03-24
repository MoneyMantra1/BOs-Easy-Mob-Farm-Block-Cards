package com.moneymantra.emfblockdrop.component;

import com.moneymantra.emfblockdrop.Constants;
import com.moneymantra.emfblockdrop.data.BlockCaptureData;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

  public static final DeferredRegister.DataComponents DATA_COMPONENTS =
      DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

  public static final Supplier<DataComponentType<BlockCaptureData>> BLOCK_CAPTURE_DATA =
      DATA_COMPONENTS.registerComponentType(
          BlockCaptureData.ID,
          builder ->
              builder
                  .persistent(BlockCaptureData.CODEC)
                  .networkSynchronized(BlockCaptureData.STREAM_CODEC));

  private ModDataComponents() {}
}
