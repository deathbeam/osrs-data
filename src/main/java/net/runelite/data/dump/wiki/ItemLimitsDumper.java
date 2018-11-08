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
import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.util.Namer;
import net.runelite.data.App;
import net.runelite.data.dump.MediaWiki;
import net.runelite.data.dump.MediaWikiTemplate;

@Slf4j
public class ItemLimitsDumper
{
	public static void dump(final Store store, final MediaWiki wiki) throws IOException
	{
		final File out = new File("runelite/runelite-client/src/main/resources/");
		out.mkdirs();

		log.info("Dumping item limits to {}", out);

		ItemManager itemManager = new ItemManager(store);
		itemManager.load();

		final Map<String, Integer> limits = new LinkedHashMap<>();

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

			if (name.isEmpty() || limits.containsKey(name))
			{
				continue;
			}

			final String data = wiki.getPageData("Module:Exchange/" + name, -1);

			if (Strings.isNullOrEmpty(data))
			{
				continue;
			}

			final MediaWikiTemplate geStats = MediaWikiTemplate.parseLua(data);

			if (geStats == null)
			{
				continue;
			}

			final int limit = geStats.getInt("limit");

			if (limit <= 0)
			{
				continue;
			}

			limits.put(name, limit);
			log.info("Dumped item limit for {} {}", item.id, name);
		}

		try (FileWriter fw = new FileWriter(new File(out, "item_limits.json")))
		{
			fw.write(App.GSON.toJson(limits));
		}

		log.info("Dumped {} item limits", limits.size());
	}
}