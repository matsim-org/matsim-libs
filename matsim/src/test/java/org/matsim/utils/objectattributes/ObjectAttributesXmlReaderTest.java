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

package org.matsim.utils.objectattributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class ObjectAttributesXmlReaderTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testParse_customConverter() throws SAXException, ParserConfigurationException, IOException {
		String tupleClass = MyTuple.class.getCanonicalName();
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
		"<objectAttributes>\n" +
		" <object id=\"one\">\n" +
		"  <attribute name=\"a\" class=\"" + tupleClass + "\">1,2</attribute>\n" +
		" </object>\n" +
		" <object id=\"two\">\n" +
		"  <attribute name=\"b\" class=\"" + tupleClass + "\">3,4</attribute>\n" +
		" </object>\n" +
		"</objectAttributes>";
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attributes);
		reader.putAttributeConverter(MyTuple.class, new MyTupleConverter());
		reader.parse(new ByteArrayInputStream(str.getBytes()));

		Object o = attributes.getAttribute("one", "a");
		Assert.assertTrue(o instanceof MyTuple);
		Assert.assertEquals(1, ((MyTuple) o).a);
		Assert.assertEquals(2, ((MyTuple) o).b);

		o = attributes.getAttribute("two", "b");
		Assert.assertTrue(o instanceof MyTuple);
		Assert.assertEquals(3, ((MyTuple) o).a);
		Assert.assertEquals(4, ((MyTuple) o).b);
	}

	@Test
	public void testParse_missingConverter() throws SAXException, ParserConfigurationException, IOException {
		String tupleClass = MyTuple.class.getCanonicalName();
		String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
		"<objectAttributes>\n" +
		" <object id=\"one\">\n" +
		"  <attribute name=\"a1\" class=\"" + tupleClass + "\">1,2</attribute>\n" +
		"  <attribute name=\"a2\" class=\"java.lang.String\">foo</attribute>\n" +
		" </object>\n" +
		" <object id=\"two\">\n" +
		"  <attribute name=\"b1\" class=\"" + tupleClass + "\">3,4</attribute>\n" +
		"  <attribute name=\"b2\" class=\"java.lang.Integer\">1980</attribute>\n" +
		" </object>\n" +
		"</objectAttributes>";
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attributes);
		reader.parse(new ByteArrayInputStream(str.getBytes()));

		Object o = attributes.getAttribute("one", "a1");
		Assert.assertNull(o);
		o = attributes.getAttribute("one", "a2");
		Assert.assertTrue(o instanceof String);
		Assert.assertEquals("foo", o);

		o = attributes.getAttribute("two", "b1");
		Assert.assertNull(o);
		o = attributes.getAttribute("two", "b2");
		Assert.assertTrue(o instanceof Integer);
		Assert.assertEquals(1980, ((Integer) o).intValue());
	}

	@Test
	public void testParse_withDtd() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getPackageInputDirectory() + "objectattributes_withDtd_v1.xml";
		ObjectAttributes oa = new ObjectAttributes();
		new ObjectAttributesXmlReader(oa).readFile(filename);

		Object o = oa.getAttribute("one", "a");
		Assert.assertTrue(o instanceof String);
		Assert.assertEquals("foobar", o);

		o = oa.getAttribute("two", "b");
		Assert.assertTrue(o instanceof Boolean);
		Assert.assertTrue(((Boolean) o).booleanValue());

		o = oa.getAttribute("two", "ccc");
		Assert.assertTrue(o instanceof Integer);
		Assert.assertEquals(42, ((Integer) o).intValue());
	}

	@Test
	public void testParse_withoutDtd() throws SAXException, ParserConfigurationException, IOException {
		String filename = this.utils.getPackageInputDirectory() + "objectattributes_withoutDtd_v1.xml";
		ObjectAttributes oa = new ObjectAttributes();
		new ObjectAttributesXmlReader(oa).readFile(filename);

		Object o = oa.getAttribute("one", "a");
		Assert.assertTrue(o instanceof String);
		Assert.assertEquals("foobar", o);

		o = oa.getAttribute("two", "b");
		Assert.assertTrue(o instanceof Boolean);
		Assert.assertTrue(((Boolean) o).booleanValue());

		o = oa.getAttribute("two", "ccc");
		Assert.assertTrue(o instanceof Integer);
		Assert.assertEquals(42, ((Integer) o).intValue());
	}

	public static class MyTuple {
		public final int a;
		public final int b;
		public MyTuple(final int a, final int b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public String toString() {
			return a + "," + b;
		}
	}

	public static class MyTupleConverter implements AttributeConverter<MyTuple> {
		@Override
		public MyTuple convert(String value) {
			String[] parts = value.split(",");
			return new MyTuple(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
		}
		@Override
		public String convertToString(Object o) {
			MyTuple t = (MyTuple) o;
			return t.a + "," + t.b;
		}
	}
}
