/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkSimSimAnalyser
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.simsimanalyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author dgrether
 *
 */
public class InvertedNetworkSimSimAnalyser {

	private static final class EventsHandler implements LinkEnterEventHandler {

		private Map<Id, LinkEnterEvent> enterEventMap = new HashMap<Id, LinkEnterEvent>();
		private Map<Tuple<String, String>, Integer> fromToCountMap = new HashMap<Tuple<String, String>, Integer>();
		
		@Override
		public void reset(int iteration) {}

		private void addFromToCount(Id from, Id to){
			Tuple<String, String> fromTo = new Tuple<String, String>(from.toString(), to.toString());
			Integer count = fromToCountMap.get(fromTo);
			if (count != null) {
				count++;
			}
			else {
				count = 1;
			}
			fromToCountMap.put(fromTo, count);
		}
		
		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getTime() < 50000.0) {
				Id vehicleId = event.getVehicleId();
				LinkEnterEvent oldEnter = enterEventMap.get(vehicleId);
				if (oldEnter != null) {
					this.addFromToCount(oldEnter.getLinkId(), event.getLinkId());
				}
				this.enterEventMap.put(vehicleId, event);
			}
		}
	}
	
	private void runAnalysis(String svndir, String networkFile, String eventsFileBaseCase,
			String eventsFilePolicy, String srs, String outfile) {
			Network network = this.loadNetwork(networkFile);
			NetworkInverter inverter = new NetworkInverter(network);
			Network  invertedNetwork = inverter.getInvertedNetwork();
			Map<Tuple<String, String>, Integer> turnMoveCountsBaseCase = this.getTurnMoveCounts(eventsFileBaseCase);
			Map<Tuple<String, String>, Integer> turnMoveCountsPolicy = this.getTurnMoveCounts(eventsFilePolicy);
			this.writeShape(invertedNetwork, srs, outfile, turnMoveCountsBaseCase, turnMoveCountsPolicy);
	}
	
	

	private Map<Tuple<String, String>, Integer> getTurnMoveCounts(String eventsFileBaseCase) {
		EventsHandler handler = new EventsHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileBaseCase);
		return handler.fromToCountMap;
	}



	private Network loadNetwork(String networkFile){
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scenario);
		return scenario.getNetwork();
	}
	
	private void writeShape(Network network, String crs, String shapefilename, Map<Tuple<String, String>, Integer> turnMoveCountsBaseCase, Map<Tuple<String, String>, Integer> turnMoveCountsPolicy) {
		CoordinateReferenceSystem networkSrs = MGC.getCRS(crs);
		PolygonFeatureFactory factory = createFeatureType(networkSrs, "1910", "1911");
		GeometryFactory geofac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Link link : network.getLinks().values()) {
			String[] ft = link.getId().toString().split("zzz");
			Tuple<String, String> t = new Tuple<String, String>(ft[0], ft[1]);
			Integer countBc = turnMoveCountsBaseCase.get(t);
			Integer countP = turnMoveCountsPolicy.get(t);
			if (countBc == null) {
				countBc = 0;
			}
			if (countP == null) {
				countP = 0;
			}
			features.add(this.createFeature(link, geofac, factory, countBc, countP));
		}
		ShapeFileWriter.writeGeometries(features, shapefilename);
	}
	
	
	
	private SimpleFeature createFeature(Link link, GeometryFactory geofac, PolygonFeatureFactory factory, int countBc, int countP) {
		Coordinate[] coords = PolygonFeatureGenerator.createPolygonCoordsForLink(link, 20.0);
		Object [] attribs = new Object[12];
		attribs[0] = link.getId().toString();
		attribs[1] = link.getFromNode().getId().toString();
		attribs[2] = link.getToNode().getId().toString();
		attribs[3] = link.getLength();
		attribs[4] = link.getFreespeed();
		attribs[5] = link.getCapacity();
		attribs[6] = link.getNumberOfLanes();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = NetworkUtils.getType(((Link) link));
		attribs[9] = countBc;
		attribs[10] = countP;
		attribs[11] = countP - countBc;
		return factory.createPolygon(coords, attribs, link.getId().toString());
	}
	
	private PolygonFeatureFactory createFeatureType(CoordinateReferenceSystem crs, String runId, String runId2) {
		PolygonFeatureFactory.Builder builder = new PolygonFeatureFactory.Builder();
		builder.setCrs(crs);
		builder.setName("link");
		builder.addAttribute("ID", String.class);
		builder.addAttribute("fromID", String.class);
		builder.addAttribute("toID", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("lanes", Double.class);
		builder.addAttribute("visWidth", Double.class);
		builder.addAttribute("type", String.class);
		builder.addAttribute("countBc", Integer.class);
		builder.addAttribute("countP", Integer.class);
		builder.addAttribute("P-Bc", Integer.class);
		return builder.create();
	}
	
	public static void main(String[] args) {
		String svndir = "/media/data/work/repos/";
		String net = null;
		String eventsFileCountValues = null;
		String eventsFileSimValues = null;
		String outfile = null;
		String srs = null;
		
		String runNr1 = "1910";
		String runNr2 = "1912";
		
		net = svndir + "runs-svn/run"+runNr1+"/"+runNr1+".output_network.xml.gz";
		eventsFileCountValues = svndir + "runs-svn/run"+runNr1+"/ITERS/it.2000/"+runNr1+".2000.events.xml.gz";
		eventsFileSimValues = svndir + "runs-svn/run"+runNr2+"/ITERS/it.2000/"+runNr2+".2000.events.xml.gz";
		outfile = svndir +  "runs-svn/run"+runNr1+"/shapefiles/"+runNr2+".2000-"+runNr1+".2000_inverted_morning_peak_analysis.shp";

		srs = TransformationFactory.WGS84_UTM33N;			
		
		InvertedNetworkSimSimAnalyser analyser = new InvertedNetworkSimSimAnalyser();
		analyser.runAnalysis(svndir, net, eventsFileCountValues, eventsFileSimValues, srs, outfile);
	}


}
