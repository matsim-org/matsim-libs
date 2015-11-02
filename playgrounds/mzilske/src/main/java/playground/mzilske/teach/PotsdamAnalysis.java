/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.teach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class PotsdamAnalysis implements Runnable {
	
	private Map<Id, AnalysisLink> linkDeltas = new HashMap<Id, AnalysisLink>();
	
	private PolylineFeatureFactory factory;
	
	public static void main(String[] args) {
		PotsdamAnalysis potsdamAnalysis = new PotsdamAnalysis();
		potsdamAnalysis.run();
	}

	@Override
	public void run() {
		String eventsFile1 = "output/ITERS/it.10/10.events.xml.gz";
		String eventsFile2 = "outputBridgeClosed/ITERS/it.20/20.events.xml.gz";
		EventsManager before = EventsUtils.createEventsManager();
		EventsManager after = EventsUtils.createEventsManager();
		
		
		String network = "output/output_network.xml.gz";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(network);
		
		
		for (Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			linkDeltas.put(entry.getKey(), new AnalysisLink());
		}
		
		before.addHandler(new Before());
		after.addHandler(new After());
		
		new MatsimEventsReader(before).readFile(eventsFile1);
		new MatsimEventsReader(after).readFile(eventsFile2);

		initFeatureType();
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			features.add(getFeature(entry.getValue()));
		}
		
		ShapeFileWriter.writeGeometries(features, "output/delta-network");
		
	}
	
	public class AnalysisLink {
		int delta;
	}
	
	public class Before implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			linkDeltas.get(event.getLinkId()).delta--;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class After implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			linkDeltas.get(event.getLinkId()).delta++;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void initFeatureType() {
		this.factory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("links").
				addAttribute("ID", String.class).
				addAttribute("flowDelta", Double.class).
				create();
	}
	
	public SimpleFeature getFeature(final Link link) {
		return this.factory.createPolyline(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())},
				new Object[] { link.getId().toString(), linkDeltas.get(link.getId()).delta}, null);
	}
	
}
