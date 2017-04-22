/* *********************************************************************** *
 * project: org.matsim.*
 * MultilayerInstanceWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.nmviljoen.io;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.GridExperiment.Archetype;

public class MultilayerInstanceWriterTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testV1() {
		GridExperiment experiment = new GridExperiment();
		experiment.setArchetype(Archetype.MALIK);
		experiment.setInstanceNumber(1);
		
		try{
			new MultilayerInstanceWriter(experiment).writeV1(utils.getOutputDirectory() + "test.xml.gz");
		} catch(Exception e){
			fail("Should write without throwing exception.");
		}
	}

}
