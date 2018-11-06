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
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
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
	@EqualsAndHashCode
	private static final class ItemEquipmentStats
	{
		int astab;
		int aslash;
		int acrush;
		int amagic;
		int arange;

		int dstab;
		int dslash;
		int dcrush;
		int dmagic;
		int drange;

		int str;
		int rstr;
		int mdmg;
		int prayer;
		int aspeed;
	}

	@EqualsAndHashCode
	private static final class ItemStats
	{
		static final ItemStats DEFAULT = new ItemStats();

		boolean quest;
		boolean equipable;
		double weight;

		ItemEquipmentStats equipment;
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

			log.info("Dumping item stat for {} {}", item.id, name);

			final ItemStats itemStat = new ItemStats();
			itemStat.quest = base.getBoolean("quest");
			itemStat.equipable = base.getBoolean("equipable");
			itemStat.weight = base.getDouble("weight");

			if (itemStat.equipable)
			{
				final MediaWikiTemplate stats = MediaWikiTemplate.parseWikitext("Infobox Bonuses", data);

				if (stats != null)
				{
					final ItemEquipmentStats equipmentStat = new ItemEquipmentStats();
					equipmentStat.astab = stats.getInt("astab");
					equipmentStat.aslash = stats.getInt("aslash");
					equipmentStat.acrush = stats.getInt("acrush");
					equipmentStat.amagic = stats.getInt("amagic");
					equipmentStat.arange = stats.getInt("arange");

					equipmentStat.dstab = stats.getInt("dstab");
					equipmentStat.dslash = stats.getInt("dslash");
					equipmentStat.dcrush = stats.getInt("dcrush");
					equipmentStat.dmagic = stats.getInt("dmagic");
					equipmentStat.drange = stats.getInt("drange");

					equipmentStat.str = stats.getInt("str");
					equipmentStat.rstr = stats.getInt("rstr");
					equipmentStat.mdmg = stats.getInt("mdmg");
					equipmentStat.prayer = stats.getInt("prayer");
					equipmentStat.aspeed = stats.getInt("aspeed");
					itemStat.equipment = equipmentStat;
				}
			}

			itemStats.put(name, itemStat);
		}

		itemStats.values().removeIf(ItemStats.DEFAULT::equals);

		try (FileWriter fw = new FileWriter(new File(out, "item_stats.json")))
		{
			fw.write(App.GSON.toJson(itemStats));
		}
	}
}
