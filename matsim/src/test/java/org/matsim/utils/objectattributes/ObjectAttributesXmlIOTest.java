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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class ObjectAttributesXmlIOTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReadWrite() throws IOException, SAXException, ParserConfigurationException {
		ObjectAttributes oa1 = new ObjectAttributes();
		oa1.putAttribute("one", "a", "A");
		oa1.putAttribute("one", "b", Integer.valueOf(1));
		oa1.putAttribute("two", "c", Double.valueOf(1.5));
		oa1.putAttribute("two", "d", Boolean.TRUE);
		new ObjectAttributesXmlWriter(oa1).writeFile(this.utils.getOutputDirectory() + "oa.xml");

		ObjectAttributes oa2 = new ObjectAttributes();
		new ObjectAttributesXmlReader(oa2).readFile(this.utils.getOutputDirectory() + "oa.xml");
		Assertions.assertEquals("A", oa2.getAttribute("one", "a"));
		Assertions.assertEquals(Integer.valueOf(1), oa2.getAttribute("one", "b"));
		Assertions.assertEquals(Double.valueOf(1.5), oa2.getAttribute("two", "c"));
		Assertions.assertEquals(Boolean.TRUE, oa2.getAttribute("two", "d"));
	}

	@Test
	void testReadWrite_CustomAttribute() {
		ObjectAttributes oa1 = new ObjectAttributes();
		MyTuple t = new MyTuple(3, 4);
		oa1.putAttribute("1", "A", t);
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(oa1);
		MyTuple.MyTupleConverter converter = new MyTuple.MyTupleConverter();
		writer.putAttributeConverter(MyTuple.class, converter);
		writer.writeFile(this.utils.getOutputDirectory() + "oa.xml");

		Assertions.assertFalse(t.toString().equals(converter.convertToString(t)), "toString() should return something different from converter to test functionality.");

		ObjectAttributes oa2 = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(oa2);
		reader.putAttributeConverter(MyTuple.class, new MyTuple.MyTupleConverter());
		reader.readFile(this.utils.getOutputDirectory() + "oa.xml");

		Object o = oa2.getAttribute("1", "A");
		Assertions.assertNotNull(o);
		Assertions.assertEquals(MyTuple.class, o.getClass());
		MyTuple t2 = (MyTuple) o;
		Assertions.assertEquals(3, t2.a);
		Assertions.assertEquals(4, t2.b);
	}

}
