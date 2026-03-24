package com.moneymantra.emfblockdrop.config;

import com.moneymantra.emfblockdrop.Constants;
import de.markusbordihn.easymobfarm.config.Config;
import java.io.File;
import java.util.Properties;

public class BlockDropFarmConfig extends Config {

  public static final String CONFIG_FILE_NAME = "block_drop_farm.cfg";
  public static final String CONFIG_FILE_HEADER =
      """
      Block Drop Farm Configuration

      captureCardChance: Chance denominator for a capture card drop when an eligible block is broken.
      Default 20000 = 0.005%% chance.

      allowFakePlayers: Allow fake players to roll for capture cards.
      dropCardRequiresSelfDrop: Require the broken block to have dropped itself in the current break context.
      excludeBlockEntities: Blacklist all block-entity / machine / container style blocks.
      excludeOresByName: Blacklist registry paths containing ore-style naming.
      """;

  public static int captureCardChance = 20000;
  public static boolean allowFakePlayers = false;
  public static boolean dropCardRequiresSelfDrop = true;
  public static boolean excludeBlockEntities = true;
  public static boolean excludeOresByName = true;

  private BlockDropFarmConfig() {}

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    captureCardChance = parseConfigValue(properties, "captureCardChance", captureCardChance);
    allowFakePlayers = parseConfigValue(properties, "allowFakePlayers", allowFakePlayers);
    dropCardRequiresSelfDrop =
        parseConfigValue(properties, "dropCardRequiresSelfDrop", dropCardRequiresSelfDrop);
    excludeBlockEntities =
        parseConfigValue(properties, "excludeBlockEntities", excludeBlockEntities);
    excludeOresByName = parseConfigValue(properties, "excludeOresByName", excludeOresByName);

    if (captureCardChance < 1) {
      captureCardChance = 1;
    }

    if (!properties.equals(unmodifiedProperties)) {
      writeConfigFile(configFile, properties, CONFIG_FILE_HEADER);
    }
  }
}
