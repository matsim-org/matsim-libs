/* *********************************************************************** *
 * project: org.matsim.*
 * SerializerDeserializerTest.java
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

package playground.gregor.sim2d_v4.io;

import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.CRS;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestCase;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


public class SerializerDeserializerTest extends MatsimTestCase {

	
	@Test
	public void testSim2DConfigSerializerDeserializer() {
		String outDir = getOutputDirectory();
		String envOut = outDir + "/env.gml.gz";
		String netOut = outDir + "/net.xml";
		String configOut = outDir +"/config.xml";
		Sim2DConfig conf = Sim2DConfigUtils.createConfig();
		conf.setEventsInterval(1);
		conf.setTimeStepSize(0.1);
		conf.addSim2DEnvironmentPath(envOut);
		conf.addSim2DEnvNetworkMapping(envOut, netOut);
		new Sim2DConfigWriter01(conf).write(configOut);
		
		Sim2DConfig conf2 = Sim2DConfigUtils.loadConfig(configOut);
		String envOut2 = conf2.getSim2DEnvironmentPaths().iterator().next();
		assertEquals(envOut, envOut2);
		String netOut2 = conf2.getNetworkPath(envOut2);
		assertEquals(netOut, netOut2);
	}
	
	@Test
	public void testSim2DEnvironmentSerializerDeserializer() {
		
		String outDir = getOutputDirectory();

		GeometryFactory geofac = new GeometryFactory();

		Coordinate c0 = new Coordinate(0,0);
		Coordinate c1 = new Coordinate(1,0);
		Coordinate c2 = new Coordinate(0,1);
		Coordinate c3 = new Coordinate(1,1);
		Coordinate [] coords = new Coordinate[]{c0,c1,c2,c3,c0};
		LinearRing lr = geofac.createLinearRing(coords);
		Polygon p = geofac.createPolygon(lr, null);
		List<Integer> sides = new ArrayList<Integer>();
		sides.add(2);
		int level = 0;

		Envelope e = new Envelope(c0,c1);
		
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(Id.create("env0", Sim2DEnvironment.class));		
		env.setEnvelope(e);
		CoordinateReferenceSystem crs = null;
		try {
			crs = CRS.decode("EPSG:3395", true);
		} catch (NoSuchAuthorityCodeException e1) {
			throw new IllegalArgumentException(e1);
		} catch (FactoryException e1) {
			throw new IllegalArgumentException(e1);
		}
		env.setCRS(crs);
		Section sec = env.createAndAddSection(Id.create("sec0", Section.class), p, new int[]{0,2}, new Id[]{Id.create("sec0", Section.class)}, level);
		sec.addRelatedLinkId(Id.create("666", Link.class));

		new Sim2DEnvironmentWriter02(env).write(outDir + "/test.gml");

		Sim2DEnvironment env2 = new Sim2DEnvironment();
		Sim2DEnvironmentReader02 reader = new Sim2DEnvironmentReader02(env2,true); //validation against gml is very slow. if someone complains we could think about switching it off [gl dec 2012]
		reader.readFile(outDir + "/test.gml");
		
		CoordinateReferenceSystem testCRS = env2.getCRS();
		assertEquals("EPSG:3395", testCRS.getIdentifiers().iterator().next().toString());
		
		Envelope testEnvelope = env2.getEnvelope();
		assertEquals(e.getMinX(),testEnvelope.getMinX(),0.0000001);
		assertEquals(e.getMinY(),testEnvelope.getMinY(),0.0000001);
		assertEquals(e.getMaxX(),testEnvelope.getMaxX(),0.0000001);
		assertEquals(e.getMaxY(),testEnvelope.getMaxY(),0.0000001);
		
		Section testSec = env2.getSections().values().iterator().next();
		assertEquals(sec.getId(), testSec.getId());
		assertEquals(sec.getLevel(), testSec.getLevel());
		assertEquals(sec.getNeighbors().length,testSec.getNeighbors().length);
		for (int i = 0; i < sec.getNeighbors().length; i++) {
			Id<Section> id = sec.getNeighbors()[i];
			Id<Section> testId = testSec.getNeighbors()[i];
			assertEquals(id,testId);
		}
		assertEquals(sec.getOpenings().length, testSec.getOpenings().length);
		for (int i = 0; i < sec.getOpenings().length; i++) {
			assertEquals(sec.getOpenings()[i],testSec.getOpenings()[i]);
		}
	
		assertEquals(sec.getRelatedLinkIds(), testSec.getRelatedLinkIds());
		for (int i = 0; i < sec.getRelatedLinkIds().size(); i++) {
			assertEquals(sec.getRelatedLinkIds().get(i),testSec.getRelatedLinkIds().get(i));
		}
	}

}
