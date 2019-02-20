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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
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
		private final Integer slot;

		private final Integer astab;
		private final Integer aslash;
		private final Integer acrush;
		private final Integer amagic;
		private final Integer arange;

		private final Integer dstab;
		private final Integer dslash;
		private final Integer dcrush;
		private final Integer dmagic;
		private final Integer drange;

		private final Integer str;
		private final Integer rstr;
		private final Integer mdmg;
		private final Integer prayer;
		private final Integer aspeed;
	}

	@Value
	@Builder
	private static final class ItemStats
	{
		static final ItemStats DEFAULT = ItemStats.builder().build();

		private final Boolean quest;
		private final Boolean equipable;
		private final Double weight;

		private final ItemEquipmentStats equipment;
	}

	public static void dump(final Store store, final MediaWiki wiki) throws IOException
	{
		final File out = new File("runelite/runelite-client/src/main/resources/");
		out.mkdirs();

		log.info("Dumping item stats to {}", out);

		final ItemManager itemManager = new ItemManager(store);
		itemManager.load();

		final Map<Integer, ItemStats> itemStats = new TreeMap<>();
		final Collection<ItemDefinition> items = itemManager.getItems();
		final Stream<ItemDefinition> itemDefinitionStream = items.parallelStream();

		itemDefinitionStream.forEach(item ->
		{
			if (item.getNotedTemplate() != -1)
			{
				return;
			}

			if (item.name.equalsIgnoreCase("NULL"))
			{
				return;
			}

			final String name = Namer
				.removeTags(item.name)
				.replace('\u00A0', ' ')
				.trim();

			if (name.isEmpty())
			{
				return;
			}

			final String data = wiki.getSpecialLookupData("item", item.id, 0);

			if (Strings.isNullOrEmpty(data))
			{
				return;
			}

			final MediaWikiTemplate base = MediaWikiTemplate.parseWikitext("Infobox Item", data);

			if (base == null)
			{
				return;
			}

			final ItemStats.ItemStatsBuilder itemStat = ItemStats.builder();
			itemStat.quest(base.getBoolean("quest"));
			itemStat.equipable(base.getBoolean("equipable"));
			itemStat.weight(base.getDouble("weight"));

			if (Boolean.TRUE.equals(itemStat.equipable))
			{
				final MediaWikiTemplate stats = MediaWikiTemplate.parseWikitext("Infobox Bonuses", data);

				if (stats != null)
				{
					final ItemEquipmentStats.ItemEquipmentStatsBuilder equipmentStat = ItemEquipmentStats.builder();
					equipmentStat.slot(toEquipmentSlot(stats.getValue("slot")));
					equipmentStat.astab(getVarInt(stats, "astab"));
					equipmentStat.aslash(getVarInt(stats, "aslash"));
					equipmentStat.acrush(getVarInt(stats, "acrush"));
					equipmentStat.amagic(getVarInt(stats, "amagic"));
					equipmentStat.arange(getVarInt(stats, "arange"));

					equipmentStat.dstab(getVarInt(stats, "dstab"));
					equipmentStat.dslash(getVarInt(stats, "dslash"));
					equipmentStat.dcrush(getVarInt(stats, "dcrush"));
					equipmentStat.dmagic(getVarInt(stats, "dmagic"));
					equipmentStat.drange(getVarInt(stats, "drange"));

					equipmentStat.str(getVarInt(stats, "str"));
					equipmentStat.rstr(getVarInt(stats, "rstr"));
					equipmentStat.mdmg(getVarInt(stats, "mdmg"));
					equipmentStat.prayer(getVarInt(stats, "prayer"));
					equipmentStat.aspeed(getVarInt(stats, "aspeed"));

					final ItemEquipmentStats builtEqStat = equipmentStat.build();

					if (!builtEqStat.equals(ItemEquipmentStats.builder().build()))
					{
						itemStat.equipment(builtEqStat);
					}
				}
			}

			final ItemStats val = itemStat.build();

			if (ItemStats.DEFAULT.equals(val))
			{
				return;
			}

			itemStats.put(item.id, val);
			log.info("Dumped item stat for {} {}", item.id, name);
		});

		try (FileWriter fw = new FileWriter(new File(out, "item_stats.json")))
		{
			fw.write(App.GSON.toJson(itemStats));
		}

		log.info("Dumped {} item stats", itemStats.size());
	}

	private static Integer getVarInt(final MediaWikiTemplate template, final String key)
	{
		final Integer var2 = template.getInt(key + "2");
		final Integer var1 = template.getInt(key + "1");
		final Integer var = template.getInt(key);

		if (var2 != null)
		{
			return var2;
		}

		if (var1 != null)
		{
			return var1;
		}

		return var;
	}

	private static Integer toEquipmentSlot(final String slotName)
	{
		if (slotName == null)
		{
			return null;
		}

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

		return null;
	}
}
