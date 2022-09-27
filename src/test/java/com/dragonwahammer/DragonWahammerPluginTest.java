package com.dragonwahammer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DragonWahammerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DragonWahammerPlugin.class);
		RuneLite.main(args);
	}
}