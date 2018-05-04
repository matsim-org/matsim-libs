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

package org.matsim.contrib.signals.data.intergreens.v10;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
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

	private Id<SignalGroup> groupId1 = Id.create("1", SignalGroup.class);
	private Id<SignalGroup> groupId2 = Id.create("2", SignalGroup.class);
	private Id<SignalGroup> groupId3 = Id.create("3", SignalGroup.class);
	private Id<SignalGroup> groupId4 = Id.create("4", SignalGroup.class);
	private Id<SignalSystem> systemId23 = Id.create("23", SignalSystem.class);
	private Id<SignalSystem> systemId42 = Id.create("42", SignalSystem.class);

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
		IntergreensForSignalSystemData ig23 = itd.getIntergreensForSignalSystemDataMap().get(systemId23);
		Assert.assertNotNull(ig23);
		Assert.assertEquals(Integer.valueOf(5), ig23.getIntergreenTime(groupId1, groupId2));
		Assert.assertEquals(Integer.valueOf(3), ig23.getIntergreenTime(groupId1, groupId3));
		Assert.assertEquals(Integer.valueOf(3), ig23.getIntergreenTime(groupId1, groupId4));
		Assert.assertNull(ig23.getIntergreenTime(groupId2, groupId3));
		
		IntergreensForSignalSystemData ig42 = itd.getIntergreensForSignalSystemDataMap().get(systemId42);
		Assert.assertNotNull(ig42);
		Assert.assertEquals(Integer.valueOf(5), ig42.getIntergreenTime(groupId1, groupId2));
		Assert.assertEquals(Integer.valueOf(3), ig42.getIntergreenTime(groupId2, groupId1));
		Assert.assertNull(ig42.getIntergreenTime(groupId1, groupId3));
		Assert.assertNull(ig42.getIntergreenTime(groupId1, groupId1));
	}

}
