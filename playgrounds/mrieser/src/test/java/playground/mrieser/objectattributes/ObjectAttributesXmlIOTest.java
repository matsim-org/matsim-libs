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

package playground.mrieser.objectattributes;

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
		new ObjectAttributesXmlReader(oa2).parse(this.utils.getOutputDirectory() + "oa.xml");
		Assert.assertEquals("A", oa2.getAttribute("one", "a"));
		Assert.assertEquals(Integer.valueOf(1), oa2.getAttribute("one", "b"));
		Assert.assertEquals(Double.valueOf(1.5), oa2.getAttribute("two", "c"));
		Assert.assertEquals(Boolean.TRUE, oa2.getAttribute("two", "d"));
	}
}
