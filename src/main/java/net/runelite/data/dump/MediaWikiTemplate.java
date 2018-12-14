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
package net.runelite.data.dump;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;

public class MediaWikiTemplate
{
	private static final Parser LUA_PARSER;
	private static final Parser MEDIAWIKI_PARSER;

	static
	{
		final Parser singleString = CharacterParser.of('\'').seq(CharacterParser.of('\'').neg().plus().flatten()).seq(CharacterParser.of('\''));
		final Parser doubleString = CharacterParser.of('"').seq(CharacterParser.of('"').neg().plus().flatten()).seq(CharacterParser.of('"'));
		final Parser string = singleString.or(doubleString).pick(1);

		final Parser key = CharacterParser.letter().or(CharacterParser.of('-')).or(CharacterParser.of('_')).or(CharacterParser.digit()).plus().flatten();
		final Parser value = string.or(key);

		final Parser pair = key.trim()
			.seq(CharacterParser.of('=').trim())
			.seq(value.trim())
			.map((Function<List<String>, Map.Entry<String, String>>) input -> new AbstractMap.SimpleEntry<>(input.get(0).trim(), input.get(2).trim()));

		final Parser commaLine = pair
			.seq(CharacterParser.of(',').optional().trim())
			.pick(0);

		LUA_PARSER = StringParser.of("return").trim()
			.seq(CharacterParser.of('{').trim())
			.seq(commaLine.plus().trim())
			.seq(CharacterParser.of('}'))
			.pick(2);

		final Parser wikiValue = CharacterParser.of('|')
			.or(StringParser.of("}}"))
			.or(StringParser.of("{{"))
			.neg().plus().trim();

		final Parser wikiExpression = StringParser.of("{{")
			.seq(StringParser.of("}}").neg().star().trim())
			.seq(StringParser.of("}}"));

		final Parser notOrPair = key.trim()
			.seq(CharacterParser.of('=').trim())
			.seq(wikiValue.or(wikiExpression).plus().flatten().trim())
			.map((Function<List<String>, Map.Entry<String, String>>) input -> new AbstractMap.SimpleEntry<>(input.get(0).trim(), input.get(2).trim()));

		final Parser orLine = CharacterParser.of('|')
			.seq(notOrPair.trim())
			.pick(1);

		MEDIAWIKI_PARSER = orLine.plus().trim().seq(StringParser.of("}}")).pick(0);
	}

	@Nullable
	public static MediaWikiTemplate parseWikitext(final String name, final String data)
	{
		final Map<String, String> out = new HashMap<>();
		final Parser wikiParser = StringParser.of("{{")
			.seq(StringParser.ofIgnoringCase(name).trim())
			.seq(MEDIAWIKI_PARSER)
			.pick(2);

		final List<Object> parsed = wikiParser.matchesSkipping(data);

		if (parsed.isEmpty())
		{
			return null;
		}

		final List<Map.Entry<String, String>> entries = (List<Map.Entry<String, String>>) parsed.get(0);

		for (Map.Entry<String, String> entry : entries)
		{
			out.put(entry.getKey(), entry.getValue());
		}

		if (out.isEmpty())
		{
			return null;
		}

		return new MediaWikiTemplate(out);
	}

	@Nullable
	public static MediaWikiTemplate parseLua(final String data)
	{
		final Map<String, String> out = new HashMap<>();
		final List<Object> parsed = LUA_PARSER.matchesSkipping(data);

		if (parsed.isEmpty())
		{
			return null;
		}

		final List<Map.Entry<String, String>> entries = (List<Map.Entry<String, String>>) parsed.get(0);

		for (Map.Entry<String, String> entry : entries)
		{
			out.put(entry.getKey(), entry.getValue());
		}

		if (out.isEmpty())
		{
			return null;
		}

		return new MediaWikiTemplate(out);
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
			val.equalsIgnoreCase("n/a") ||
			val.equals("nil") ||
			val.equalsIgnoreCase("varies"))
		{
			return null;
		}

		val = val.replace("kg", "").replaceAll("[><]", "");
		return Strings.isNullOrEmpty(val) ? null : val;
	}

	public Boolean getBoolean(final String key)
	{
		final String val = getValue(key);
		return !Strings.isNullOrEmpty(val) ? true : null;
	}

	public Double getDouble(final String key)
	{
		final String val = getValue(key);

		if (Strings.isNullOrEmpty(val))
		{
			return null;
		}

		try
		{
			double v = Double.parseDouble(val);
			return v != 0 ? v : null;
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public Integer getInt(final String key)
	{
		final String val = getValue(key);

		if (Strings.isNullOrEmpty(val))
		{
			return null;
		}

		try
		{
			int v = Integer.parseInt(val);
			return v != 0 ? v : null;
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
