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

package org.matsim.signalsystems.data.intergreens.v10;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 * 
 */
public class IntergreenTimesData10ReaderWriterTest {

	private static final Logger log = Logger.getLogger(IntergreenTimesData10ReaderWriterTest.class);

	private static final String TESTXML = "testIntergreenTimes_v1.0.xml";

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	private Id id1 = new IdImpl("1");
	private Id id2 = new IdImpl("2");
	private Id id3 = new IdImpl("3");
	private Id id4 = new IdImpl("4");
	private Id id23 = new IdImpl("23");
	private Id id42 = new IdImpl("42");
	

	@Test
	public void testParser() throws IOException, JAXBException, SAXException,
			ParserConfigurationException {
		IntergreenTimesData atd = new IntergreenTimesDataImpl();
		IntergreenTimesReader10 reader = new IntergreenTimesReader10(atd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
		checkContent(atd);

	}

	@Test
	public void testWriter() throws JAXBException, SAXException, ParserConfigurationException,
			IOException {
		String testoutput = this.testUtils.getOutputDirectory() + "testAtdOutput.xml";
		log.debug("reading file...");
		// read the test file
		IntergreenTimesData atd = new IntergreenTimesDataImpl();
		IntergreenTimesReader10 reader = new IntergreenTimesReader10(atd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);

		// write the test file
		log.debug("write the test file...");
		IntergreenTimesWriter10 writer = new IntergreenTimesWriter10(atd);
		writer.write(testoutput);

		log.debug("and read it again");
		atd = new IntergreenTimesDataImpl();
		reader = new IntergreenTimesReader10(atd);
		reader.readFile(testoutput);
		checkContent(atd);
	}

	private void checkContent(IntergreenTimesData itd) {
		Assert.assertNotNull(itd);
		Assert.assertNotNull(itd.getIntergreensForSignalSystemDataMap());
		Assert.assertEquals(2, itd.getIntergreensForSignalSystemDataMap().size());
		IntergreensForSignalSystemData ig23 = itd.getIntergreensForSignalSystemDataMap().get(id23);
		Assert.assertNotNull(ig23);
		Assert.assertEquals(Integer.valueOf(5), ig23.getIntergreenTime(id1, id2));
		Assert.assertEquals(Integer.valueOf(3), ig23.getIntergreenTime(id1, id3));
		Assert.assertEquals(Integer.valueOf(3), ig23.getIntergreenTime(id1, id4));
		Assert.assertNull(ig23.getIntergreenTime(id2, id3));
		
		IntergreensForSignalSystemData ig42 = itd.getIntergreensForSignalSystemDataMap().get(id42);
		Assert.assertNotNull(ig42);
		Assert.assertEquals(Integer.valueOf(5), ig42.getIntergreenTime(id1, id2));
		Assert.assertEquals(Integer.valueOf(3), ig42.getIntergreenTime(id2, id1));
		Assert.assertNull(ig42.getIntergreenTime(id1, id3));
		Assert.assertNull(ig42.getIntergreenTime(id1, id1));
	}

}
