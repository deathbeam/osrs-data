/*
 * MIT License
 *
 * Copyright (c) 2018 Tomas Slusny
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy*
 *
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
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.util.Namer;
import net.runelite.data.App;
import net.runelite.data.dump.MediaWiki;
import net.runelite.data.dump.MediaWikiTemplate;

public class ItemStatsDumper
{
	private static final class ItemStats
	{
		boolean quest;
		boolean equipable;
		double weight;

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

	public static void dump(final Store store, final MediaWiki wiki) throws IOException
	{
		final File out = new File("runelite/runelite-client/src/main/resources/");
		out.mkdirs();

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

			final String data = wiki.getPageData(name);

			if (Strings.isNullOrEmpty(data))
			{
				continue;
			}

			System.out.println("Dumping item " + item.id + " " + name);

			final ItemStats itemStat = new ItemStats();
			final MediaWikiTemplate base = MediaWikiTemplate.parse("Infobox Item", data);

			if (base == null)
			{
				continue;
			}

			itemStat.quest = base.getBoolean("quest");
			itemStat.equipable = base.getBoolean("equipable");
			itemStat.weight = base.getDouble("weight");

			final MediaWikiTemplate stats = MediaWikiTemplate.parse("Infobox Bonuses", data);

			if (stats != null)
			{
				itemStat.astab = stats.getInt("astab");
				itemStat.aslash = stats.getInt("aslash");
				itemStat.acrush = stats.getInt("acrush");
				itemStat.amagic = stats.getInt("amagic");
				itemStat.arange = stats.getInt("arange");

				itemStat.dstab = stats.getInt("dstab");
				itemStat.dslash = stats.getInt("dslash");
				itemStat.dcrush = stats.getInt("dcrush");
				itemStat.dmagic = stats.getInt("dmagic");
				itemStat.drange = stats.getInt("drange");

				itemStat.str = stats.getInt("str");
				itemStat.rstr = stats.getInt("rstr");
				itemStat.mdmg = stats.getInt("mdmg");
				itemStat.prayer = stats.getInt("prayer");
				itemStat.aspeed = stats.getInt("aspeed");
			}

			itemStats.put(name, itemStat);
		}

		try (FileWriter fw = new FileWriter(new File(out, "item_stats.json")))
		{
			fw.write(App.GSON.toJson(itemStats));
		}
	}
}
