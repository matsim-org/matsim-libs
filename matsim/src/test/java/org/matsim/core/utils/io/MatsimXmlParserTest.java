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
import org.xml.sax.SAXParseException;

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

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
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

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
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
		String str = """
			<?xml version='1.0' encoding='UTF-8'?>\r
			<root>\r
			<dummy someAttribute="value1">content</dummy>\r
			<dummy2 someAttribute2="value2">content2</dummy2>\r
			</root>""";

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
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

		public TestParser(ValidationType validationType) {
			super(validationType);
		}

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

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
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
	public void testParse_parseEntities() {
		String xml = """
			<?xml version='1.0' encoding='UTF-8'?>
			<!DOCTYPE a SYSTEM "network_v1.dtd" [
			<!ENTITY B_VALUE  "b2">
			]>
			<a>
			<b>b1</b>
			<b>&B_VALUE;</b>
			</a>""";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();
		new MatsimXmlParser(MatsimXmlParser.ValidationType.DTD_ONLY) {
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
	public void testParse_dtdValidation() {
		String xml = """
			<?xml version='1.0' encoding='UTF-8'?>
			<!DOCTYPE network SYSTEM "network_v2.dtd" ><network>
			<nodes>
			<node id="abc" x="123" y="abc" />
			<link id="def" from="abc" to="def" length="123" freespeed="13.33" capacity="2000" permlanes="1" />
			</nodes>
			</network>""";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();

		try {
			new MatsimXmlParser(MatsimXmlParser.ValidationType.DTD_ONLY) {
				@Override
				public void startTag(String name, Attributes atts, Stack<String> context) {
					log.add(name);
				}

				@Override
				public void endTag(String name, String content, Stack<String> context) {
				}
			}.parse(stream);
			Assert.fail("expected exception.");
		} catch (UncheckedIOException e) {
			Assert.assertTrue(e.getCause() instanceof SAXParseException); // expected
		}

		Assert.assertEquals(3, log.size());
		Assert.assertEquals("network", log.get(0));
		Assert.assertEquals("nodes", log.get(1));
		Assert.assertEquals("node", log.get(2));
	}

	@Test
	public void testParse_xsdValidationSuccess() {
		String xml = """
			<?xml version="1.0" encoding="UTF-8"?>

			<vehicleDefinitions xmlns="http://www.matsim.org/files/dtd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/vehicleDefinitions_v2.0.xsd">
				<vehicleType id="bus">
					<capacity seats="70" standingRoomInPersons="0" />
					<length meter="18.0"/>
					<width meter="2.5"/>
				</vehicleType>
			</vehicleDefinitions>
			""";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();
		new MatsimXmlParser(MatsimXmlParser.ValidationType.XSD_ONLY) {
			@Override
			public void startTag(String name, Attributes atts, Stack<String> context) {
				log.add(name);
			}

			@Override
			public void endTag(String name, String content, Stack<String> context) {
			}
		}.parse(stream);

		Assert.assertEquals(5, log.size());
		Assert.assertEquals("vehicleDefinitions", log.get(0));
		Assert.assertEquals("vehicleType", log.get(1));
		Assert.assertEquals("capacity", log.get(2));
		Assert.assertEquals("length", log.get(3));
		Assert.assertEquals("width", log.get(4));
	}

	@Test
	public void testParse_xsdValidationFailure() {
		String xml = """
			<?xml version="1.0" encoding="UTF-8"?>

			<vehicleDefinitions xmlns="http://www.matsim.org/files/dtd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/vehicleDefinitions_v2.0.xsd">
				<vehicleType id="bus">
					<capacity seats="70" standingRoomInPersons="0" />
					<length meter="18.0"/>
					<width meter="2.5"/>
					<foo meter="123.4"/> <!-- must not exist -->
				</vehicleType>
			</vehicleDefinitions>
			""";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();

		try {
			new MatsimXmlParser(MatsimXmlParser.ValidationType.XSD_ONLY) {
				@Override
				public void startTag(String name, Attributes atts, Stack<String> context) {
					log.add(name);
				}

				@Override
				public void endTag(String name, String content, Stack<String> context) {
				}
			}.parse(stream);
			Assert.fail("expected exception.");
		} catch (UncheckedIOException e) {
			Assert.assertTrue(e.getCause() instanceof SAXParseException); // expected
		}

		Assert.assertEquals(5, log.size());
		Assert.assertEquals("vehicleDefinitions", log.get(0));
		Assert.assertEquals("vehicleType", log.get(1));
		Assert.assertEquals("capacity", log.get(2));
		Assert.assertEquals("length", log.get(3));
		Assert.assertEquals("width", log.get(4));
	}

	@Test
	public void testParse_preventXEEattack_woodstox() throws IOException {
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
		try {
			new MatsimXmlParser(MatsimXmlParser.ValidationType.DTD_ONLY) {
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
			Assert.fail("Expected exception, got none.");
		} catch (UncheckedIOException expected) {}

		Assert.assertEquals(2, log.size());
		Assert.assertEquals("b1", log.get(0));
		Assert.assertEquals(" - b2 - ", log.get(1));
	}

	@Test
	public void testParse_preventXEEattack_xerces() throws IOException {
		// XEE: XML eXternal Entity attack: https://en.wikipedia.org/wiki/XML_external_entity_attack

		String secretValue = "S3CR3T";

		File secretsFile = this.tempFolder.newFile("file-with-secrets.txt");
		try (OutputStream out = new FileOutputStream(secretsFile)) {
			out.write(secretValue.getBytes(StandardCharsets.UTF_8));
		}

		String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<!DOCTYPE objectattributes SYSTEM \"objectattributes_v1.dtd\" [\n" +
				"<!ENTITY A_VALUE  \"a2\">\n" +
				"<!ENTITY SECRET_VALUE SYSTEM \"file://" + secretsFile.getAbsolutePath() + "\">\n" +
				"]>\n" +
				"<objectattributes>\n" +
				"<object id=\"one\">\n" +
				"<attribute name=\"a\" class=\"java.lang.String\">&A_VALUE;</attribute>\n" +
				"<attribute name=\"b\" class=\"java.lang.String\">&SECRET_VALUE;</attribute>\n" +
				"</object>\n" +
				"</objectattributes>";

		InputStream stream = new ByteArrayInputStream(xml.getBytes());
		final List<String> log = new ArrayList<>();
		new MatsimXmlParser(MatsimXmlParser.ValidationType.DTD_OR_XSD) {
			{
				this.setValidating(true);
			}

			@Override
			public void startTag(String name, Attributes atts, Stack<String> context) {
			}

			@Override
			public void endTag(String name, String content, Stack<String> context) {
				System.out.println(name + "-" + content);
				log.add(name + "-" + content);
			}
		}.parse(stream);

		Assert.assertEquals(4, log.size());
		Assert.assertEquals("attribute-a2", log.get(0));
		Assert.assertEquals("attribute-", log.get(1));
		Assert.assertEquals("object-", log.get(2));
		Assert.assertEquals("objectattributes-", log.get(3));
	}

}
