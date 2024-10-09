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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author mrieser / Simunto
 */
public class MatsimXmlParserTest {

	@TempDir
	public File tempFolder;

	@Test
	void testParsingReservedEntities_AttributeValue() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<dummy someAttribute=\"value&quot;&amp;&lt;&gt;value\">content</dummy>";

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
		parser.setValidating(false);

		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assertions.assertEquals("dummy", parser.lastStartTag);
		Assertions.assertEquals("dummy", parser.lastEndTag);
		Assertions.assertEquals("content", parser.lastContent);
		Assertions.assertEquals(1, parser.lastAttributes.getLength());
		Assertions.assertEquals("someAttribute", parser.lastAttributes.getLocalName(0));
		Assertions.assertEquals("value\"&<>value", parser.lastAttributes.getValue(0));
		Assertions.assertEquals("value\"&<>value", parser.lastAttributes.getValue("someAttribute"));
	}

	@Test
	void testParsingReservedEntities_Content() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<dummy someAttribute=\"value\">content&quot;&amp;&lt;&gt;content</dummy>";

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
		parser.setValidating(false);

		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assertions.assertEquals("dummy", parser.lastStartTag);
		Assertions.assertEquals("dummy", parser.lastEndTag);
		Assertions.assertEquals("content\"&<>content", parser.lastContent);
		Assertions.assertEquals(1, parser.lastAttributes.getLength());
		Assertions.assertEquals("someAttribute", parser.lastAttributes.getLocalName(0));
		Assertions.assertEquals("value", parser.lastAttributes.getValue(0));
		Assertions.assertEquals("value", parser.lastAttributes.getValue("someAttribute"));
	}

	/**
	 * Tests that reading XML files with CRLF as newline characters works as expected.
	 * Based on a (non-reproducible) bug message on the users-mailing list 2012-04-26.
	 */
	@Test
	void testParsing_WindowsLinebreaks() {
		String str = """
			<?xml version='1.0' encoding='UTF-8'?>\r
			<root>\r
			<dummy someAttribute="value1">content</dummy>\r
			<dummy2 someAttribute2="value2">content2</dummy2>\r
			</root>""";

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
		parser.setValidating(false);

		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assertions.assertEquals("dummy2", parser.lastStartTag);
		Assertions.assertEquals("root", parser.lastEndTag);
		Assertions.assertEquals(1, parser.lastAttributes.getLength());
		Assertions.assertEquals("someAttribute2", parser.lastAttributes.getLocalName(0));
		Assertions.assertEquals("value2", parser.lastAttributes.getValue(0));
		Assertions.assertEquals("value2", parser.lastAttributes.getValue("someAttribute2"));
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
	void testParsingPlusSign() {
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
				"<dummy someAttribute=\"value+value\">content+content</dummy>";

		TestParser parser = new TestParser(MatsimXmlParser.ValidationType.DTD_ONLY);
		parser.setValidating(false);

		parser.parse(new ByteArrayInputStream(str.getBytes()));
		Assertions.assertEquals("dummy", parser.lastStartTag);
		Assertions.assertEquals("dummy", parser.lastEndTag);
		Assertions.assertEquals("content+content", parser.lastContent);
		Assertions.assertEquals(1, parser.lastAttributes.getLength());
		Assertions.assertEquals("someAttribute", parser.lastAttributes.getLocalName(0));
		Assertions.assertEquals("value+value", parser.lastAttributes.getValue(0));
		Assertions.assertEquals("value+value", parser.lastAttributes.getValue("someAttribute"));
	}

	@Test
	void testParse_parseEntities() {
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

		Assertions.assertEquals("b1", log.get(0));
		Assertions.assertEquals("b2", log.get(1));
	}

	@Test
	void testParse_dtdValidation() {
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
			Assertions.fail("expected exception.");
		} catch (UncheckedIOException e) {
			Assertions.assertTrue(e.getCause() instanceof IOException); // expected
			Assertions.assertTrue(e.getCause().getCause() instanceof SAXParseException); // expected
		}

		Assertions.assertEquals(3, log.size());
		Assertions.assertEquals("network", log.get(0));
		Assertions.assertEquals("nodes", log.get(1));
		Assertions.assertEquals("node", log.get(2));
	}

	@Test
	void testParse_xsdValidationSuccess() {
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

		Assertions.assertEquals(5, log.size());
		Assertions.assertEquals("vehicleDefinitions", log.get(0));
		Assertions.assertEquals("vehicleType", log.get(1));
		Assertions.assertEquals("capacity", log.get(2));
		Assertions.assertEquals("length", log.get(3));
		Assertions.assertEquals("width", log.get(4));
	}

	@Test
	void testParse_xsdValidationFailure() {
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
			Assertions.fail("expected exception.");
		} catch (UncheckedIOException e) {
			Assertions.assertTrue(e.getCause() instanceof IOException); // expected
			Assertions.assertTrue(e.getCause().getCause() instanceof SAXParseException); // expected
		}

		Assertions.assertEquals(5, log.size());
		Assertions.assertEquals("vehicleDefinitions", log.get(0));
		Assertions.assertEquals("vehicleType", log.get(1));
		Assertions.assertEquals("capacity", log.get(2));
		Assertions.assertEquals("length", log.get(3));
		Assertions.assertEquals("width", log.get(4));
	}

	@Test
	void testParse_preventXEEattack_woodstox() throws IOException {
		// XEE: XML eXternal Entity attack: https://en.wikipedia.org/wiki/XML_external_entity_attack

		String secretValue = "S3CR3T";

		File secretsFile = new File(this.tempFolder,"file-with-secrets.txt");
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
			Assertions.fail("Expected exception, got none.");
		} catch (UncheckedIOException expected) {}

		Assertions.assertEquals(2, log.size());
		Assertions.assertEquals("b1", log.get(0));
		Assertions.assertEquals(" - b2 - ", log.get(1));
	}

	@Test
	void testParse_preventXEEattack_xerces() throws IOException {
		// XEE: XML eXternal Entity attack: https://en.wikipedia.org/wiki/XML_external_entity_attack

		String secretValue = "S3CR3T";

		File secretsFile = new File(this.tempFolder, "file-with-secrets.txt");
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

		Assertions.assertEquals(4, log.size());
		Assertions.assertEquals("attribute-a2", log.get(0));
		Assertions.assertEquals("attribute-", log.get(1));
		Assertions.assertEquals("object-", log.get(2));
		Assertions.assertEquals("objectattributes-", log.get(3));
	}

}
