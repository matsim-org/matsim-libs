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

package org.matsim.contrib.signals.data.ambertimes.v10;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * @author jbischoff
 * @author dgrether
 *
 */
public class AmberTimesData10ReaderWriterTest {

	private static final Logger log = LogManager.getLogger(AmberTimesData10ReaderWriterTest.class);

	private static final String TESTXML = "testAmberTimes_v1.0.xml";

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testParser() throws IOException, JAXBException, SAXException,
			ParserConfigurationException {
		AmberTimesData atd = new AmberTimesDataImpl();
		AmberTimesReader10 reader = new AmberTimesReader10(atd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
		checkContent(atd);

	}

	@Test
	void testWriter() throws JAXBException, SAXException, ParserConfigurationException,
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
		Assertions.assertNotNull(ats);
		Assertions.assertEquals(0.3, ats.getDefaultAmberTimeGreen(), 1e-7);
		Assertions.assertEquals(1, ats.getDefaultRedAmber().intValue());
		Assertions.assertEquals(4, ats.getDefaultAmber().intValue());
		// system id1 defaults
		AmberTimeData atdata = ats.getAmberTimeDataBySystemId().get(Id.create(1, SignalSystem.class));
		Assertions.assertNotNull(atdata);
		Assertions.assertEquals(1, atdata.getDefaultRedAmber().intValue());
		Assertions.assertEquals(4, atdata.getDefaultAmber().intValue());
		// Signal 1 defaults
		Assertions.assertNotNull(atdata.getAmberOfSignal(Id.create(1, Signal.class)));
		Assertions.assertNotNull(atdata.getSignalAmberMap());
		Assertions.assertNotNull(atdata.getSignalRedAmberMap());
		Assertions.assertEquals(4, atdata.getAmberOfSignal(Id.create(1, Signal.class)).intValue());
		Assertions.assertEquals(2, atdata.getRedAmberOfSignal(Id.create(1, Signal.class)).intValue());

	}

}
