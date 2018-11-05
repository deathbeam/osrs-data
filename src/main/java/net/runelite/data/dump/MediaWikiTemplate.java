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
package net.runelite.data.dump;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class MediaWikiTemplate
{
	@Nullable
	public static MediaWikiTemplate parse(final String name, final String data)
	{
		final String[] split = data.split("\n");
		final Map<String, String> out = new HashMap<>();

		boolean hasStart = false;

		for (String line : split)
		{
			if (line.startsWith("{{" + name))
			{
				hasStart = true;
				continue;
			}

			if (!hasStart)
			{
				continue;
			}

			if (line.endsWith("}}"))
			{
				return new MediaWikiTemplate(out);
			}

			if (line.startsWith("|"))
			{
				final String[] kv = line.substring(1).split("=");

				if (kv.length != 2)
				{
					continue;
				}

				out.put(kv[0].trim(), kv[1].trim());
			}
		}

		return null;
	}

	private final Map<String, String> map;

	private MediaWikiTemplate(final Map<String, String> map)
	{
		this.map = map;
	}

	public String getValue(final String key)
	{
		String val = map.get(key);

		if (Strings.isNullOrEmpty(val) ||
			val.equalsIgnoreCase("no") ||
			val.equalsIgnoreCase("n/a"))
		{
			return "";
		}

		val = val.replace("kg", "").replaceAll("[><]", "");
		return val;
	}

	public boolean getBoolean(final String key)
	{
		final String val = getValue(key);

		if (Strings.isNullOrEmpty(val))
		{
			return false;
		}

		return !"no".equalsIgnoreCase(val);
	}

	public double getDouble(final String key)
	{
		final String val = getValue(key);

		if (Strings.isNullOrEmpty(val))
		{
			return 0;
		}

		try
		{
			return Double.parseDouble(val);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return 0;
		}
	}

	public int getInt(final String key)
	{
		final String val = getValue(key);

		if (Strings.isNullOrEmpty(val))
		{
			return 0;
		}

		try
		{
			return Integer.parseInt(val);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return 0;
		}
	}
}
