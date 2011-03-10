/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationAnalysis.java
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
package playground.gregor.evacuation.destination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class DestinationAnalysis implements AgentDepartureEventHandler, AgentArrivalEventHandler {
	
	private ScenarioImpl sc;
	
	private final double blurFactor = 0.25;

	Map<Id,AgentInfo> agents = new HashMap<Id, AgentInfo>();
	
	public DestinationAnalysis(ScenarioImpl scenario) {
		this.sc = scenario;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		AgentInfo ai = new AgentInfo();
		ai.origin = this.sc.getNetwork().getLinks().get(event.getLinkId()).getCoord();
		ai.length = this.sc.getNetwork().getLinks().get(event.getLinkId()).getLength();
		agents.put(event.getPersonId(), ai);		
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.agents.get(event.getPersonId()).destination = event.getLinkId();
		
	}
	
	private static class AgentInfo {
		Coord origin;
		Id destination;
		double length = 0;
	}
	
	private void createShapeFile(String output) {
		FeatureType ft = initFeatureType();
		List<Feature> fts = new ArrayList<Feature>();
		
		GeometryFactory geofac = new GeometryFactory();
		for (AgentInfo ai : this.agents.values() ) {
			if (ai.destination == null) {
				System.err.println("AAAA");
				continue;
			}
			try {
				Point p = geofac.createPoint(new Coordinate(ai.origin.getX()+blurFactor*2*(MatsimRandom.getRandom().nextDouble()-0.5)*ai.length, ai.origin.getY()+blurFactor*2*(MatsimRandom.getRandom().nextDouble()-0.5)*ai.length));
				fts.add(ft.create(new Object[]{p, ai.destination.toString()}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}

		try {
			ShapeFileWriter.writeGeometries(fts, output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private FeatureType initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
		AttributeType dest = AttributeTypeFactory.newAttributeType("destination", String.class);
//		AttributeType agDep = AttributeTypeFactory.newAttributeType("agDep", Integer.class);
//		AttributeType agLost = AttributeTypeFactory.newAttributeType("agLost", Integer.class);
//		AttributeType agLostRate = AttributeTypeFactory.newAttributeType("agLostRate", Double.class);
//		AttributeType agLostPerc = AttributeTypeFactory.newAttributeType("agLostPerc", Integer.class);
//		AttributeType agLostPercStr = AttributeTypeFactory.newAttributeType("agLostPercStr", String.class);
//		AttributeType agLabel = AttributeTypeFactory.newAttributeType("agLabel", String.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, dest}, "Destination");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}
	
	public static void main(String [] args) {
		
//		String base = "/home/laemmel/devel/allocation/";
		String base = "/home/laemmel/arbeit/svn/runs-svn/run1062/";
		String events = base + "output/ITERS/it.2000/2000.events.txt.gz";
		String output = base + "analysis/destColors2000.shp";
		String network = base + "output/output_network.xml.gz";
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(network);
		
		DestinationAnalysis da = new DestinationAnalysis(scenario);
		
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(da);
		new MatsimEventsReader(em).readFile(events);
		
		da.createShapeFile(output);
		
	}





}
