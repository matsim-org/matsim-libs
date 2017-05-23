/* *********************************************************************** *
 * project: org.matsim.*
 * MultilayerInstanceReaderTeast.java
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

public class MultilayerInstanceReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	/**
	 * The read file should be the same example as the one used to test the
	 * writing of the multilayer network.
	 */
	public void testV1() {
		GridExperiment experiment = new GridExperiment();
		
		try{
			new MultilayerInstanceReader(experiment).readFile(utils.getClassInputDirectory() + "test.xml.gz");
		} catch(Exception e){
			fail("Should read without exception");
		}
		
		assertEquals("Wrong archetype.", Archetype.NAVABI, experiment.getArchetype());
		assertEquals("Wrong instance number.", 123l, experiment.getInstanceNumber());
		
		assertNotNull("Should have physical network.", experiment.getPhysicalNetwork());
		assertEquals("Wrong number of nodes.", 3, experiment.getPhysicalNetwork().getVertexCount());
		assertEquals("Wrong number of edges.", 4, experiment.getPhysicalNetwork().getEdgeCount());

		assertNotNull("Should have logical network.", experiment.getPhysicalNetwork());
		assertEquals("Wrong number of nodes.", 2, experiment.getLogicalNetwork().getVertexCount());
		assertEquals("Wrong number of edges.", 2, experiment.getLogicalNetwork().getEdgeCount());
		
		assertEquals("Wrong association.", "1", experiment.getLogicalNodeFromPhysical("1"));
		assertEquals("Wrong association.", null, experiment.getLogicalNodeFromPhysical("2"));
		assertEquals("Wrong association.", "2", experiment.getLogicalNodeFromPhysical("3"));
		assertEquals("Wrong association.", "1", experiment.getPhysicalNodeFromLogical("1"));
		assertEquals("Wrong association.", "3", experiment.getPhysicalNodeFromLogical("2"));
		
		assertEquals("Wrong number of shortest path sets.", 2, experiment.getShortestPathSets().size());
		assertNotNull("Should find shortest parh set from '1' to '2'.", experiment.getShortestPathSets().get("1").get("2"));
		assertNotNull("Should find shortest parh set from '2' to '1'.", experiment.getShortestPathSets().get("2").get("1"));		
	}

}
