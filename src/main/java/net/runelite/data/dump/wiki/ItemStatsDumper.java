/*
 * MIT License
 *
 * Copyright (c) 2018 Tomas Slusny
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.runelite.data.dump.wiki;

import com.google.common.base.Strings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.util.Namer;
import net.runelite.data.App;
import net.runelite.data.dump.MediaWiki;
import net.runelite.data.dump.MediaWikiTemplate;

@Slf4j
public class ItemStatsDumper
{
    @Value
	@Builder
	private static final class ItemEquipmentStats
	{
		private final int slot;

		private final int astab;
		private final int aslash;
		private final int acrush;
		private final int amagic;
		private final int arange;

		private final int dstab;
		private final int dslash;
		private final int dcrush;
		private final int dmagic;
		private final int drange;

		private final int str;
		private final int rstr;
		private final int mdmg;
		private final int prayer;
		private final int aspeed;
	}

    @Value
	@Builder
	private static final class ItemStats
	{
		static final ItemStats DEFAULT = ItemStats.builder().build();

		private final boolean quest;
		private final boolean equipable;
		private final double weight;

		private final ItemEquipmentStats equipment;
	}

	public static void dump(final Store store, final MediaWiki wiki) throws IOException
	{
		final File out = new File("runelite/runelite-client/src/main/resources/");
		out.mkdirs();

		log.info("Dumping item stats to {}", out);

		ItemManager itemManager = new ItemManager(store);
		itemManager.load();

        final Map<String, ItemStats> itemStats = new LinkedHashMap<>();

		for (ItemDefinition item : itemManager.getItems())
		{
			if (item.name.equalsIgnoreCase("NULL"))
			{
				continue;
			}

			final String name = Namer
				.removeTags(item.name)
				.replace('\u00A0', ' ')
				.trim();

			if (name.isEmpty() || itemStats.containsKey(name))
			{
				continue;
			}

			final String data = wiki.getPageData(name, 0);

			if (Strings.isNullOrEmpty(data))
			{
				continue;
			}

			final MediaWikiTemplate base = MediaWikiTemplate.parseWikitext("Infobox Item", data);

			if (base == null)
			{
				continue;
			}

			final ItemStats.ItemStatsBuilder itemStat = ItemStats.builder();
			itemStat.quest(base.getBoolean("quest"));
			itemStat.equipable(base.getBoolean("equipable"));
			itemStat.weight(base.getDouble("weight"));

			if (itemStat.equipable)
			{
				final MediaWikiTemplate stats = MediaWikiTemplate.parseWikitext("Infobox Bonuses", data);

				if (stats != null)
				{
					final ItemEquipmentStats.ItemEquipmentStatsBuilder equipmentStat = ItemEquipmentStats.builder();
					equipmentStat.slot(toEquipmentSlot(stats.getValue("slot")));
					equipmentStat.astab(stats.getInt("astab"));
					equipmentStat.aslash(stats.getInt("aslash"));
					equipmentStat.acrush(stats.getInt("acrush"));
					equipmentStat.amagic(stats.getInt("amagic"));
					equipmentStat.arange(stats.getInt("arange"));

					equipmentStat.dstab(stats.getInt("dstab"));
					equipmentStat.dslash(stats.getInt("dslash"));
					equipmentStat.dcrush(stats.getInt("dcrush"));
					equipmentStat.dmagic(stats.getInt("dmagic"));
					equipmentStat.drange(stats.getInt("drange"));

					equipmentStat.str(stats.getInt("str"));
					equipmentStat.rstr(stats.getInt("rstr"));
					equipmentStat.mdmg(stats.getInt("mdmg"));
					equipmentStat.prayer(stats.getInt("prayer"));
					equipmentStat.aspeed(stats.getInt("aspeed"));
					itemStat.equipment(equipmentStat.build());
				}
			}

			itemStats.put(name, itemStat.build());
			log.info("Dumped item stat for {} {}", item.id, name);
		}

		itemStats.values().removeIf(ItemStats.DEFAULT::equals);

		try (FileWriter fw = new FileWriter(new File(out, "item_stats.json")))
		{
			fw.write(App.GSON.toJson(itemStats));
		}

		log.info("Dumped {} item stats", itemStats.size());
	}

	private static int toEquipmentSlot(final String slotName)
	{
		switch (slotName.toLowerCase())
		{
			case "weapon":
			case "2h":
				// TODO: 2h should return both weapon and shield somehow
				return EquipmentInventorySlot.WEAPON.getSlotIdx();
			case "body":
				return EquipmentInventorySlot.BODY.getSlotIdx();
			case "head":
				return EquipmentInventorySlot.HEAD.getSlotIdx();
			case "ammo":
				return EquipmentInventorySlot.AMMO.getSlotIdx();
			case "legs":
				return EquipmentInventorySlot.LEGS.getSlotIdx();
			case "feet":
				return EquipmentInventorySlot.BOOTS.getSlotIdx();
			case "hands":
				return EquipmentInventorySlot.GLOVES.getSlotIdx();
			case "cape":
				return EquipmentInventorySlot.CAPE.getSlotIdx();
			case "neck":
				return EquipmentInventorySlot.AMULET.getSlotIdx();
			case "ring":
				return EquipmentInventorySlot.RING.getSlotIdx();
			case "shield":
				return EquipmentInventorySlot.SHIELD.getSlotIdx();
		}

		return -1;
	}
}
