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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MediaWikiTemplateTest
{
	@Test
	void parseWikitext()
	{
		final String infoboxItemData =
			"{{Infobox Item\n" +
			"|name = Dragon claws\n" +
			"|image = [[File:Dragon claws.png]]\n" +
			"|release = [[5 January]] [[2017]]\n" +
			"|update = Dragon Claws & 3rd Birthday\n" +
			"|members = Yes\n" +
			"|quest = No\n" +
			"|tradeable = Yes\n" +
			"|equipable = Yes\n" +
			"|stackable = No\n" +
			"|high = 123000\n" +
			"|low = 82000\n" +
			"|destroy = Drop\n" +
			"|store = No\n" +
			"|exchange = gemw\n" +
			"|examine = A set of fighting claws.\n" +
			"|weight = 0\n" +
			"}}\n";

		final MediaWikiTemplate infoboxItem = MediaWikiTemplate.parseWikitext("Infobox Item", infoboxItemData);
		assertNotNull(infoboxItem);
		assertEquals(infoboxItem.getInt("high"), 123000);

		final String infoboxBonusesData =
			"{{Infobox Bonuses\n" +
			"|astab = 41\n" +
			"|aslash = 57\n" +
			"|acrush = -4\n" +
			"|amagic = 0\n" +
			"|arange = 0\n" +
			"|dstab = 13\n" +
			"|dslash = 26\n" +
			"|dcrush = 7\n" +
			"|dmagic = 0\n" +
			"|drange = 0\n" +
			"|str = 56\n" +
			"|rstr = 0\n" +
			"|mdmg = 0\n" +
			"|prayer = 0\n" +
			"|caption = A player wearing dragon claws.\n" +
			"|aspeed = 4|slot = 2h\n" +
			"|image = Dragon claws equipped.png{{!}}130px}}\n";

		final MediaWikiTemplate infoboxBonuses = MediaWikiTemplate.parseWikitext("Infobox Bonuses", infoboxBonusesData);
		assertNotNull(infoboxBonuses);
		assertEquals(infoboxBonuses.getInt("aspeed"), 4);
		assertEquals(infoboxBonuses.getValue("slot"), "2h");
		assertEquals(infoboxBonuses.getValue("image"), "Dragon claws equipped.png{{!}}130px");

		final String multiItemInfoboxBonusesData =
			"{{Infobox Bonuses\n" +
			"|version1 = Uncharged\n" +
			"|version2 = Charged\n" +
			"|image_1 = Dragonfire shield (uncharged) equipped.png{{!}}150px\n" +
			"|image_2 = Dragonfire shield equipped.png{{!}}150px\n" +
			"|astab = 0\n" +
			"|aslash = 0\n" +
			"|acrush = 0\n" +
			"|amagic = -10\n" +
			"|arange = -5\n" +
			"|dstab1 = +20\n" +
			"|dslash1 = +25\n" +
			"|dcrush1 = +22\n" +
			"|dmagic1 = +10\n" +
			"|drange1 = +22\n" +
			"|dstab2 = +70\n" +
			"|dslash2 = +75\n" +
			"|dcrush2 = +72\n" +
			"|dmagic2 = +10\n" +
			"|drange2 = +72\n" +
			"|str = +7\n" +
			"|rstr = 0\n" +
			"|mdmg = 0\n" +
			"|prayer = 0\n" +
			"|slot = Shield\n" +
			"}}";

		final MediaWikiTemplate multiBonuses = MediaWikiTemplate.parseWikitext("Infobox Bonuses", multiItemInfoboxBonusesData);
		assertNotNull(multiBonuses);
		assertEquals(multiBonuses.getInt("dstab2"), 70);
	}

	@Test
	void parseLua()
	{
		final String exchangeInfoData =
			"return {\n" +
			"    itemId     = 13652,\n" +
			"    price      = 83173735,\n" +
			"    last       = 83533604,\n" +
			"    date       = '12:18, November 08, 2018 (UTC)',\n" +
			"    lastDate   = '05:43, November 08, 2018 (UTC)',\n" +
			"    icon       = 'Dragon claws.png',\n" +
			"    item       = 'Dragon claws',\n" +
			"    value      = -205000,\n" +
			"    limit      = nil,\n" +
			"    members    = true,\n" +
			"    category   = nil,\n" +
			"    examine    = 'A set of fighting claws.'\n" +
			"}\n";

		final MediaWikiTemplate exchangeInfo = MediaWikiTemplate.parseLua(exchangeInfoData);
		assertNotNull(exchangeInfo);
		assertEquals(exchangeInfo.getInt("value"), -205000);
	}
}