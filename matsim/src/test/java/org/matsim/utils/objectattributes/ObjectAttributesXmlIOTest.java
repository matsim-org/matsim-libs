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
public class ObjectAttributesXmlIOTest {

	@Rule	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReadWrite() throws IOException, SAXException, ParserConfigurationException {
		ObjectAttributes oa1 = new ObjectAttributes();
		oa1.putAttribute("one", "a", "A");
		oa1.putAttribute("one", "b", Integer.valueOf(1));
		oa1.putAttribute("two", "c", Double.valueOf(1.5));
		oa1.putAttribute("two", "d", Boolean.TRUE);
		new ObjectAttributesXmlWriter(oa1).writeFile(this.utils.getOutputDirectory() + "oa.xml");

		ObjectAttributes oa2 = new ObjectAttributes();
		new ObjectAttributesXmlReader(oa2).readFile(this.utils.getOutputDirectory() + "oa.xml");
		Assert.assertEquals("A", oa2.getAttribute("one", "a"));
		Assert.assertEquals(Integer.valueOf(1), oa2.getAttribute("one", "b"));
		Assert.assertEquals(Double.valueOf(1.5), oa2.getAttribute("two", "c"));
		Assert.assertEquals(Boolean.TRUE, oa2.getAttribute("two", "d"));
	}
	
	@Test
	public void testReadWrite_CustomAttribute() {
		ObjectAttributes oa1 = new ObjectAttributes();
		MyTuple t = new MyTuple(3, 4);
		oa1.putAttribute("1", "A", t);
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(oa1);
		MyTupleConverter converter = new MyTupleConverter();
		writer.putAttributeConverter(MyTuple.class, converter);
		writer.writeFile(this.utils.getOutputDirectory() + "oa.xml");
		
		Assert.assertFalse("toString() should return something different from converter to test functionality.", t.toString().equals(converter.convertToString(t)));

		ObjectAttributes oa2 = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(oa2);
		reader.putAttributeConverter(MyTuple.class, new MyTupleConverter());
		reader.readFile(this.utils.getOutputDirectory() + "oa.xml");
		
		Object o = oa2.getAttribute("1", "A");
		Assert.assertNotNull(o);
		Assert.assertEquals(MyTuple.class, o.getClass());
		MyTuple t2 = (MyTuple) o;
		Assert.assertEquals(3, t2.a);
		Assert.assertEquals(4, t2.b);
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
			return a + "/" + b;
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
			return t.a + "," + t.b; // make it something different from MyTuple.toString()
		}
	}
}
