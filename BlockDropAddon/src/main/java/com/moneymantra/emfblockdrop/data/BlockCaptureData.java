package com.moneymantra.emfblockdrop.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record BlockCaptureData(ResourceLocation blockId) {

  public static final String ID = "block_capture_data";
  public static final Codec<BlockCaptureData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(ResourceLocation.CODEC.fieldOf("blockId").forGetter(BlockCaptureData::blockId))
                  .apply(instance, BlockCaptureData::new));
  public static final StreamCodec<RegistryFriendlyByteBuf, BlockCaptureData> STREAM_CODEC =
      StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, BlockCaptureData::blockId, BlockCaptureData::new);
}
