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


package org.matsim.contrib.signals.data.signalgroups.v20;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
* @author jbischoff
* @author dgrether
*
*/
public class SignalGroups20ReaderWriterTest {
	private static final Logger log = LogManager.getLogger(SignalGroups20ReaderWriterTest.class);

	private static final String TESTXML = "testSignalGroups_v2.0.xml";

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	private Id<SignalSystem> id23 = Id.create("23", SignalSystem.class);
	private Id<Signal> id1 = Id.create("1", Signal.class);
	private Id<Signal> id4 = Id.create("4", Signal.class);
	private Id<Signal> id5 = Id.create("5", Signal.class);
	private Id<SignalSystem> id42 = Id.create("42", SignalSystem.class);

	private Id<SignalGroup> idSg1 = Id.create("1", SignalGroup.class);
	private Id<SignalGroup> idSg2 = Id.create("2", SignalGroup.class);


	@Test
	void testParser() throws IOException, JAXBException, SAXException,
			ParserConfigurationException {
		SignalGroupsData sgd = new SignalGroupsDataImpl();
		SignalGroupsReader20 reader = new SignalGroupsReader20(sgd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
		checkContent(sgd);

	}

	@Test
	void testWriter() throws JAXBException, SAXException, ParserConfigurationException,
			IOException {
		String testoutput = this.testUtils.getOutputDirectory() + "testSgOutput.xml";
		log.debug("reading file...");
		// read the test file
		SignalGroupsData sgd = new SignalGroupsDataImpl();
		SignalGroupsReader20 reader = new SignalGroupsReader20(sgd);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);

		// write the test file
		log.debug("write the test file...");
		SignalGroupsWriter20 writer = new SignalGroupsWriter20(sgd);
		writer.write(testoutput);

		log.debug("and read it again");
		sgd = new SignalGroupsDataImpl();
		reader = new SignalGroupsReader20(sgd);
		reader.readFile(testoutput);
		checkContent(sgd);
	}



	private void checkContent(SignalGroupsData sgd) {
		Assertions.assertNotNull(sgd);
		Assertions.assertNotNull(sgd.getSignalGroupDataBySignalSystemId());
		Assertions.assertNotNull(sgd.getSignalGroupDataBySystemId(id23));

		//sg23
		Map<Id<SignalGroup>,SignalGroupData> ss23 = sgd.getSignalGroupDataBySystemId(id23);
		Assertions.assertEquals(id23,ss23.get(idSg1).getSignalSystemId());

		Set<Id<Signal>> sg = ss23.get(idSg1).getSignalIds();
		Assertions.assertTrue(sg.contains(id1));

		//sg42
		Assertions.assertNotNull(sgd.getSignalGroupDataBySystemId(id42));
		Map<Id<SignalGroup>,SignalGroupData> ss42 = sgd.getSignalGroupDataBySystemId(id42);
		Assertions.assertEquals(id42,ss42.get(idSg1).getSignalSystemId());

		sg =  ss42.get(idSg1).getSignalIds();
		Assertions.assertTrue(sg.contains(id1));
		sg =  ss42.get(idSg2).getSignalIds();
		Assertions.assertTrue(sg.contains(id1));
		Assertions.assertTrue(sg.contains(id4));
		Assertions.assertTrue(sg.contains(id5));





		}


	}





