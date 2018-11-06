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
package net.runelite.data.dump.cache;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.util.Namer;
import net.runelite.data.App;

@Slf4j
public class ItemVariationsDumper
{
	public static void dump(final Store store) throws IOException
	{
		final File out = new File("runelite/runelite-client/src/main/resources/");
		out.mkdirs();

		log.info("Dumping item variations to {}", out);

		ItemManager itemManager = new ItemManager(store);
		itemManager.load();

		final Multimap<String, Integer> mmap = LinkedListMultimap.create();

		for (ItemDefinition def : itemManager.getItems())
		{
			final String outName = Namer.removeTags(def.name).toLowerCase()
				.replace("null", "")
				.replaceAll("\\([^)]+\\)", "")
				.replaceAll("[^a-zA-Z0-9 ]", "")
				.replaceAll(" [0-9]+|[0-9]+ ", "")
				.replaceAll("uncharged | uncharged", "")
				.replaceAll("new | new", "")
				.replaceAll(" full", "")
				.replaceAll("half a ", "")
				.replaceAll("part ", "")
				.replace("  ", " ")
				.replaceAll("\\w+ slayer helmet", "slayer helmet")
				.replaceAll("\\w+ abyssal whip", "abyssal whip")
				.replaceAll("magma helm|tanzanite helm", "serpentine helm")
				.replace("trident of the seas", "trident")
				.replace("trident of the swamp", "toxic trident")
				.replace("toxic staff of the dead", "toxic staff")
				.trim();

			if (outName.isEmpty())
			{
				continue;
			}

			mmap.put(outName, def.id);
		}

		final Map<String, Collection<Integer>> map = mmap.asMap();
		map.entrySet().removeIf(entry -> entry.getValue().size() <= 1);
		try (FileWriter fw = new FileWriter(new File(out, "item_variations.json")))
		{
			fw.write(App.GSON.toJson(map));
		}

		log.info("Dumped {} item variations", map.size());
	}
}
