/* *********************************************************************** *
 * project: org.matsim.*
 * CustomizedOSM2Sim2DTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.helper;

import java.util.Map.Entry;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sim2d_v4.io.Sim2DEnvironmentReader02;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Envelope;

public class CustomizedOSM2Sim2DTest extends MatsimTestCase{

	@Test
	public void testCustomizedOSM2Sim2D() {
		String inDir = getInputDirectory();

		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);

		Config c0 = ConfigUtils.createConfig();
		Scenario sc0 = ScenarioUtils.createScenario(c0);
		sc0.addScenarioElement(Sim2DScenario.ELEMENT_NAME, s2dsc);
		CustomizedOSM2Sim2DExtendedMATSimScenario osm2sim2d = new CustomizedOSM2Sim2DExtendedMATSimScenario(sc0);
		osm2sim2d.processOSMFile(inDir + "/sim2d.osm");
		
		
		//now read the same environment from gml
		Sim2DEnvironment env2 = new Sim2DEnvironment();
		Sim2DEnvironmentReader02 reader = new Sim2DEnvironmentReader02(env2,false);
		reader.readFile(inDir + "/sim2d_env.gml.gz");
		
		//compare them

		CoordinateReferenceSystem testCRS = env2.getCRS();
		assertEquals("EPSG:3395", testCRS.getIdentifiers().iterator().next().toString());

		
		Sim2DEnvironment env = s2dsc.getSim2DEnvironments().iterator().next();
		
		Envelope e1 = env.getEnvelope();
		Envelope e2 = env2.getEnvelope();
		assertEquals(e1.getMinX(),e2.getMinX(),0.0000001);
		assertEquals(e1.getMinY(),e2.getMinY(),0.0000001);
		assertEquals(e1.getMaxX(),e2.getMaxX(),0.0000001);
		assertEquals(e1.getMaxY(),e2.getMaxY(),0.0000001);

		for (Entry<Id<Section>, Section> entry : env2.getSections().entrySet()) {
			Section sec2 = entry.getValue();
			Section sec = env.getSections().get(sec2.getId());

			assertEquals(sec2.getId(), sec.getId());
			assertEquals(sec2.getLevel(), sec.getLevel());
			if (sec2.getNeighbors() != null && sec.getNeighbors() != null)  {
				assertEquals(sec2.getNeighbors().length,sec.getNeighbors().length);

				for (int i = 0; i < sec2.getNeighbors().length; i++) {
					Id<Section> id2 = sec2.getNeighbors()[i];
					Id<Section> id =  sec.getNeighbors()[i];
					assertEquals(id2,id);
				} 
			}
			assertEquals(sec2.getOpenings().length, sec.getOpenings().length);
			for (int i = 0; i < sec2.getOpenings().length; i++) {
				assertEquals(sec2.getOpenings()[i],sec.getOpenings()[i]);
			}
		}
		
//		//read the network
//		Config c = ConfigUtils.createConfig();
//		Scenario sc = ScenarioUtils.createScenario(c);
//		new MatsimNetworkReader(sc).readFile(inDir + "/network.xml.gz");
//		Network net = env.getEnvironmentNetwork();
//		Network net2 = sc.getNetwork();
//		//compare them
//		assertEquals(net.getNodes().size(), net2.getNodes().size());
//		for (Node n : net.getNodes().values()) {
//			Node n2 = net2.getNodes().get(n.getId());
//			assertEquals(n.getCoord(), n2.getCoord());
//		}
//		assertEquals(net.getLinks().size(),net2.getLinks().size());
//		for (Link l : net.getLinks().values()) {
//			Link l2 = net2.getLinks().get(l.getId());
//			assertEquals(l.getCapacity(), l2.getCapacity());
//			assertEquals(l.getFreespeed(), l2.getFreespeed());
//			assertEquals(l.getLength(), l2.getLength());
//			assertEquals(l.getNumberOfLanes(), l2.getNumberOfLanes());
//			assertEquals(l.getFromNode().getId(), l2.getFromNode().getId());
//			assertEquals(l.getToNode().getId(), l2.getToNode().getId());
//		}
		
		

	}

}
