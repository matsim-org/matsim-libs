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

package org.matsim.signalsystems.data.ambertimes.v10;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author jbischoff
 * @author dgrether
 * 
 */
public class AmberTimesData10ReaderWriterTest {

	private static final Logger log = Logger.getLogger(AmberTimesData10ReaderWriterTest.class);

	private static final String TESTXML = "testAmberTimes_v1.0.xml";

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	private Id id1 = new IdImpl("1");

	@Test
	public void testParser() throws IOException, JAXBException, SAXException,
			ParserConfigurationException {
		AmberTimesData atd = new AmberTimesDataImpl();
		AmberTimesReader10 reader = new AmberTimesReader10(atd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
		checkContent(atd);

	}

	@Test
	public void testWriter() throws JAXBException, SAXException, ParserConfigurationException,
			IOException {
		String testoutput = this.testUtils.getOutputDirectory() + "testAtdOutput.xml";
		log.debug("reading file...");
		// read the test file
		AmberTimesData atd = new AmberTimesDataImpl();
		AmberTimesReader10 reader = new AmberTimesReader10(atd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);

		// write the test file
		log.debug("write the test file...");
		AmberTimesWriter10 writer = new AmberTimesWriter10(atd);
		writer.write(testoutput);

		log.debug("and read it again");
		atd = new AmberTimesDataImpl();
		reader = new AmberTimesReader10(atd);
		reader.readFile(testoutput);
		checkContent(atd);
	}

	private void checkContent(AmberTimesData ats) {
		// global defaults
		Assert.assertNotNull(ats);
		Assert.assertEquals(0.3, ats.getDefaultAmberTimeGreen());
		Assert.assertEquals(1, ats.getDefaultRedAmber().intValue());
		Assert.assertEquals(4, ats.getDefaultAmber().intValue());
		// system id1 defaults
		AmberTimeData atdata = ats.getAmberTimeDataBySystemId().get(id1);
		Assert.assertNotNull(atdata);
		Assert.assertEquals(1, atdata.getDefaultRedAmber().intValue());
		Assert.assertEquals(4, atdata.getDefaultAmber().intValue());
		// Signal 1 defaults
		Assert.assertNotNull(atdata.getAmberOfSignal(id1));
		Assert.assertNotNull(atdata.getSignalAmberMap());
		Assert.assertNotNull(atdata.getSignalRedAmberMap());
		Assert.assertEquals(4, atdata.getAmberOfSignal(id1).intValue());
		Assert.assertEquals(2, atdata.getRedAmberOfSignal(id1).intValue());

	}

}
