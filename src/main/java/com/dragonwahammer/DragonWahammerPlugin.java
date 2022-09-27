package com.dragonwahammer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Hitsplat;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

@Slf4j
@PluginDescriptor(
	name = "Dragon WAHammer",
	enabledByDefault = false,
	description = "Swaps out the special attack sound on the dragon warhammer for something a bit more... WAHnderful."
)
public class DragonWahammerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DragonWahammerConfig config;

	private Clip clip;

	private String wahPath = "wah.wav";
	private String mlemPath = "mlem.wav";
	private SpecialWeapon dwh = SpecialWeapon.DRAGON_WARHAMMER;

	@Provides
    DragonWahammerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DragonWahammerConfig.class);
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		if (event.getSoundId() == 2520)
		{
			event.consume();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
		Player player = client.getLocalPlayer();
		Actor actor = hitsplatApplied.getActor();

		if (!player.getName().equals(actor.getName()))
		{
			if (usedDwh()) {
				Hitsplat hitsplat = hitsplatApplied.getHitsplat();

				if (hitsplat.isMine()) {
					if (hitsplat.getAmount() > 0) {
						playSound(wahPath);
					} else {
						playSound(mlemPath);
					}
				}
			}
		}
	}

	public void playSound(String sound)
	{
		try {
			if (clip != null)
			{
				clip.close();
			}

			Class pluginClass = null;
			AudioInputStream stream = null;
			try {
				pluginClass = Class.forName("com.dragonwahammer.DragonWahammerPlugin");
				URL url = pluginClass.getClassLoader().getResource(sound);
				stream = AudioSystem.getAudioInputStream(url);
			} catch (ClassNotFoundException | UnsupportedAudioFileException | IOException e) {
				e.printStackTrace();
			}

			if (stream == null)
			{
				return;
			}

			AudioFormat format = stream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			clip = (Clip) AudioSystem.getLine(info);

			clip.open(stream);

			FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float volumeValue = volume.getMinimum() + ((50 + (config.volumeLevel()*5)) * ((volume.getMaximum() - volume.getMinimum()) / 100));

			volume.setValue(volumeValue);

			clip.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean usedDwh()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return false;
		}

		Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null)
		{
			return false;
		}

		if (Arrays.stream(dwh.getItemID()).anyMatch(id -> id == weapon.getId()))
		{
			return true;
		}
		return false;
	}
}
