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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.Attributes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author mrieser / Simunto
 */
public class MatsimXmlParserTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

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

	@Test
	public void testParse_parseEntities() throws IOException {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<!DOCTYPE a SYSTEM \"network_v1.dtd\" [\n" +
				"<!ENTITY B_VALUE  \"b2\">\n" +
				"]>\n" +
				"<a>\n" +
				"<b>b1</b>\n" +
				"<b>&B_VALUE;</b>\n" +
				"</a>";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();
		new MatsimXmlParser() {
			{
				this.setValidating(false);
			}
			@Override
			public void startTag(String name, Attributes atts, Stack<String> context) {
			}
			@Override
			public void endTag(String name, String content, Stack<String> context) {
				log.add(content);
			}
		}.parse(stream);

		Assert.assertEquals("b1", log.get(0));
		Assert.assertEquals("b2", log.get(1));
	}

	@Test
	public void testParse_preventXEEattack() throws IOException {
		// XEE: XML eXternal Entity attack: https://en.wikipedia.org/wiki/XML_external_entity_attack

		String secretValue = "S3CR3T";

		File secretsFile = this.tempFolder.newFile("file-with-secrets.txt");
		try (OutputStream out = new FileOutputStream(secretsFile)) {
			out.write(secretValue.getBytes(StandardCharsets.UTF_8));
		}

		String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<!DOCTYPE a SYSTEM \"network_v1.dtd\" [\n" +
				"<!ENTITY B_VALUE  \"b2\">\n" +
				"<!ENTITY SECRET_VALUE SYSTEM \"file://" + secretsFile.getAbsolutePath() + "\">\n" +
				"]>\n" +
				"<a>\n" +
				"<b>b1</b>\n" +
				"<b> - &B_VALUE; - </b>\n" +
				"<b> - &SECRET_VALUE; - </b>\n" +
				"</a>";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();
		new MatsimXmlParser() {
			{
				this.setValidating(false);
			}
			@Override
			public void startTag(String name, Attributes atts, Stack<String> context) {
			}
			@Override
			public void endTag(String name, String content, Stack<String> context) {
				log.add(content);
			}
		}.parse(stream);

		Assert.assertEquals("b1", log.get(0));
		Assert.assertEquals(" - b2 - ", log.get(1));
		Assert.assertEquals(" -  - ", log.get(2));
	}


}
