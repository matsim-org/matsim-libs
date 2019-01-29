/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.io.ByteArrayInputStream;
import java.util.Stack;

import org.junit.Assert;

import org.junit.Test;
import org.xml.sax.Attributes;

/**
 * @author mrieser / senozon
 */
public class MatsimXmlParserTest {

	@Test
	public void testParsingReservedEntities_AttributeValue() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<dummy someAttribute=\"value&quot;&amp;&lt;&gt;value\">content</dummy>";
		
		TestParser parser = new TestParser();
		parser.setValidating(false);
		
		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assert.assertEquals("dummy", parser.lastStartTag);
		Assert.assertEquals("dummy", parser.lastEndTag);
		Assert.assertEquals("content", parser.lastContent);
		Assert.assertEquals(1, parser.lastAttributes.getLength());
		Assert.assertEquals("someAttribute", parser.lastAttributes.getLocalName(0));
		Assert.assertEquals("value\"&<>value", parser.lastAttributes.getValue(0));
		Assert.assertEquals("value\"&<>value", parser.lastAttributes.getValue("someAttribute"));
	}

	@Test
	public void testParsingReservedEntities_Content() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<dummy someAttribute=\"value\">content&quot;&amp;&lt;&gt;content</dummy>";
		
		TestParser parser = new TestParser();
		parser.setValidating(false);
		
		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assert.assertEquals("dummy", parser.lastStartTag);
		Assert.assertEquals("dummy", parser.lastEndTag);
		Assert.assertEquals("content\"&<>content", parser.lastContent);
		Assert.assertEquals(1, parser.lastAttributes.getLength());
		Assert.assertEquals("someAttribute", parser.lastAttributes.getLocalName(0));
		Assert.assertEquals("value", parser.lastAttributes.getValue(0));
		Assert.assertEquals("value", parser.lastAttributes.getValue("someAttribute"));
	}

	/**
	 * Tests that reading XML files with CRLF as newline characters works as expected.
	 * Based on a (non-reproducible) bug message on the users-mailing list 2012-04-26. 
	 */
	@Test
	public void testParsing_WindowsLinebreaks() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\r\n" +
				"<root>\r\n" +
				"<dummy someAttribute=\"value1\">content</dummy>\r\n" +
				"<dummy2 someAttribute2=\"value2\">content2</dummy2>\r\n" +
				"</root>";

		TestParser parser = new TestParser();
		parser.setValidating(false);
		
		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assert.assertEquals("dummy2", parser.lastStartTag);
		Assert.assertEquals("root", parser.lastEndTag);
		Assert.assertEquals(1, parser.lastAttributes.getLength());
		Assert.assertEquals("someAttribute2", parser.lastAttributes.getLocalName(0));
		Assert.assertEquals("value2", parser.lastAttributes.getValue(0));
		Assert.assertEquals("value2", parser.lastAttributes.getValue("someAttribute2"));
	}
	
	private static class TestParser extends MatsimXmlParser {

		public String lastStartTag = null;
		public String lastEndTag = null;
		public Attributes lastAttributes = null;
		public String lastContent = null;
		
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			this.lastStartTag = name;
			this.lastAttributes = atts;
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			this.lastEndTag = name;
			this.lastContent = content;
		}
		
	}
	
	@Test
	public void testParsingPlusSign() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<dummy someAttribute=\"value+value\">content+content</dummy>";
		
		TestParser parser = new TestParser();
		parser.setValidating(false);

		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assert.assertEquals("dummy", parser.lastStartTag);
		Assert.assertEquals("dummy", parser.lastEndTag);
		Assert.assertEquals("content+content", parser.lastContent);
		Assert.assertEquals(1, parser.lastAttributes.getLength());
		Assert.assertEquals("someAttribute", parser.lastAttributes.getLocalName(0));
		Assert.assertEquals("value+value", parser.lastAttributes.getValue(0));
		Assert.assertEquals("value+value", parser.lastAttributes.getValue("someAttribute"));
	}

}
