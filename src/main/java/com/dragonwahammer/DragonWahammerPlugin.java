package com.dragonwahammer;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.*;
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

	private static final String HIT_NAME = "dwh_hit.wav";
	private static final String MISS_NAME = "dwh_miss.wav";
	private static final File HIT_FILE = new File(RuneLite.RUNELITE_DIR, HIT_NAME);
	private static final File MISS_FILE = new File(RuneLite.RUNELITE_DIR, MISS_NAME);

	private int specPercentage = -1;
	private boolean isSpec = false;

	private SpecialWeapon dwh = SpecialWeapon.DRAGON_WARHAMMER;

	@Provides
	DragonWahammerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DragonWahammerConfig.class);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		if (event.getVarpId() != VarPlayer.SPECIAL_ATTACK_PERCENT.getId()) {
			return;
		}

		int newSpecPercentage = event.getValue();
		if (specPercentage == -1 || newSpecPercentage >= specPercentage) {
			specPercentage = newSpecPercentage;
		} else if (newSpecPercentage < specPercentage) {
			specPercentage = newSpecPercentage;
			if (usedDwh()) {
				isSpec = true;
			}
		}


	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		if (event.getSoundId() == 2520 && isSpec)
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
			if (isSpec) {
				isSpec = false;
				Hitsplat hitsplat = hitsplatApplied.getHitsplat();

				if (hitsplat.isMine()) {
					if (hitsplat.getAmount() > 0) {
						playCustomSound(true);
					} else {
						playCustomSound(false);
					}
				}
			}
		}
	}

	private synchronized void playCustomSound(boolean hit) {
		File file = hit ? HIT_FILE : MISS_FILE;
		try {
			if (clip != null) {
				clip.close();
			}

			clip = AudioSystem.getClip();

			if (!tryLoadSound(hit)) {
				return;
			}

			FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float volumeValue = volume.getMinimum() + ((50 + (config.volumeLevel() * 5)) * ((volume.getMaximum() - volume.getMinimum()) / 100));

			volume.setValue(volumeValue);

			clip.loop(0);
		} catch (LineUnavailableException e) {
			log.warn("Unable to play custom sound", e);
			return;
		}
	}

	private boolean tryLoadSound(boolean hit)
	{
		File file = hit ? HIT_FILE : MISS_FILE;
		String filename = hit ? HIT_NAME : MISS_NAME;
		if (file.exists())
		{
			try (InputStream fileStream = new BufferedInputStream(new FileInputStream(file));
				 AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream))
			{
				clip.open(sound);
				return true;
			}
			catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
			{
				log.warn("Unable to load custom sound", e);
			}
		}

		// Otherwise load from the classpath
		try (InputStream fileStream = new BufferedInputStream(Notifier.class.getResourceAsStream(filename));
			 AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream))
		{
			clip.open(sound);
			return true;
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
		{
			log.warn("Unable to load custom sound", e);
		}
		return false;
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
