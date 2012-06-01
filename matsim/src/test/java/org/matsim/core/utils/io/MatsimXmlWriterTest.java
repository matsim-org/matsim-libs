/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.utils.io;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser
 */
public class MatsimXmlWriterTest {

	@Test
	public void testEncodeAttributeValue() {
		MatsimXmlWriter writer = new MatsimXmlWriter() { };
		Assert.assertEquals("hello world!", writer.encodeAttributeValue("hello world!"));
		Assert.assertEquals("you &amp; me", writer.encodeAttributeValue("you & me"));
		Assert.assertEquals("you &amp; me &amp; her", writer.encodeAttributeValue("you & me & her"));
		Assert.assertEquals("tick &quot; tack", writer.encodeAttributeValue("tick \" tack"));
		Assert.assertEquals("tick &quot; tack &quot; tock", writer.encodeAttributeValue("tick \" tack \" tock"));
		Assert.assertEquals("this &amp; that &quot; these &amp; those", writer.encodeAttributeValue("this & that \" these & those"));
		Assert.assertEquals("tick &lt; tack &gt; tock", writer.encodeAttributeValue("tick < tack > tock"));
	}

	@Test
	public void testEncodedContent() {
		MatsimXmlWriter writer = new MatsimXmlWriter() { };
		Assert.assertEquals("hello world!", writer.encodeContent("hello world!"));
		Assert.assertEquals("you &amp; me", writer.encodeContent("you & me"));
		Assert.assertEquals("you &amp; me &amp; her", writer.encodeContent("you & me & her"));
		Assert.assertEquals("tick \" tack", writer.encodeContent("tick \" tack"));
		Assert.assertEquals("tick \" tack \" tock", writer.encodeContent("tick \" tack \" tock"));
		Assert.assertEquals("this &amp; that \" these &amp; those", writer.encodeContent("this & that \" these & those"));
		Assert.assertEquals("tick &lt; tack &gt; tock", writer.encodeContent("tick < tack > tock"));
	}
	
}
