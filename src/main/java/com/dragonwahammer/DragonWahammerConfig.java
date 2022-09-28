package com.dragonwahammer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("dragonwahammer")
public interface DragonWahammerConfig extends Config
{
	@Range(
			min = 1,
			max = 10
	)
	@ConfigItem(
		keyName = "volumeLevel",
		name = "Volume",
		description = "Adjust special attack volume"
	)
	default int volumeLevel() {
		return 10;
	}
}
