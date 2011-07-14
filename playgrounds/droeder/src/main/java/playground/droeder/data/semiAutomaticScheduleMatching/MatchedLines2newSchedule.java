/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.data.semiAutomaticScheduleMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.droeder.DaFileReader;
import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class MatchedLines2newSchedule {
	private static final Logger log = Logger
			.getLogger(MatchedLines2newSchedule.class);
	
	private ScenarioImpl osm;
	private TransitSchedule hafas;
	
	/**
	 * @param osm
	 * @param hafas
	 */
	public MatchedLines2newSchedule(ScenarioImpl osm, TransitSchedule hafas) {
		this.osm = osm;
		this.hafas = hafas;
	}

	public static void main(String[] args){
		final String DIR = DaPaths.OUTPUT + "osm/";
		final String MATCHEDLINES = DIR + "manuallyMatchedRoutes.csv";
		
		final String OSMNET = DIR + "osm_berlin_subway_net.xml";
		final String OSMSCHED = DIR + "osm_berlin_subway_sched.xml";
		final String HAFASSCHED = DaPaths.OUTPUT + "bvg09/transitSchedule-HAFAS-Coord.xml";
		final String OUTSCHED = DIR + "manuallyMatchedSched.xml";
		
		final Set<String[]> matchedLines = DaFileReader.readFileContent(MATCHEDLINES, ";", true);
		
		
		ScenarioImpl osm = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(osm).readFile(OSMNET);
		osm.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(osm).readFile(OSMSCHED);
		
		ScenarioImpl hafas = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hafas.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(hafas).readFile(HAFASSCHED); 
		
		MatchedLines2newSchedule schedMaker = new MatchedLines2newSchedule(osm, hafas.getTransitSchedule());
		TransitSchedule newSchedule = schedMaker.run(matchedLines);
		new TransitScheduleWriter(newSchedule).writeFile(OUTSCHED);
	}

	/**
	 * osm2HafasLine <code>String[]</code> should look like this
	 * [0]==osmId	[1]==hafasId
	 * 
	 * 
	 * @param osm2HafasLine
	 * @return
	 */
	public TransitSchedule run(Set<String[]> osm2HafasLine) {
		ScenarioImpl newSc = ((ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		newSc.getConfig().scenario().setUseTransit(true);
		
		TransitSchedule newSchedule = newSc.getTransitSchedule();
		
		for(String[] lines : osm2HafasLine){
			if(!(lines[0] == null || lines[1] == null)){
				createNewLine(newSchedule, lines);
			}
		}
		return newSchedule;
	}

	/**
	 * @param newSchedule
	 * @param lines
	 */
	private void createNewLine(TransitSchedule newSchedule, String[] lines) {
		TransitLine osm, hafas;
		osm = this.osm.getTransitSchedule().getTransitLines().get(new IdImpl(lines[0]));
		hafas = this.hafas.getTransitLines().get(new IdImpl(lines[1]));
		
		NetworkImpl networkForThisLine = NetworkImpl.createNetwork();
		
		// create temporary Network for the line based on osm
		for(TransitRoute r : osm.getRoutes().values()){
			createNetworkForThisLine(networkForThisLine, r);
		}
		
		// create the routes from the hafassSchedule
		TransitLine newLine = newSchedule.getFactory().createTransitLine(hafas.getId());
		for(TransitRoute r : hafas.getRoutes().values()){
			newLine.addRoute(createNewRoute(r, networkForThisLine, newSchedule));
		}
		
	}
	
	/**
	 * @param r
	 * @param networkForThisRoute
	 * @param newSchedule
	 * @return 
	 */
	private TransitRoute createNewRoute(TransitRoute r, NetworkImpl networkForThisRoute,
			TransitSchedule newSchedule) {
		TransitScheduleFactory factory = newSchedule.getFactory();
		TransitStopFacility facility;
		TransitRouteStop stop;
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		List<Id> links = new ArrayList<Id>();
		
		boolean first = true;
		for(TransitRouteStop s : r.getStops()){
			stop = createNewStop(newSchedule, s, networkForThisRoute);
			stops.add(stop);
			if(first == true){
				links.add(stop.getStopFacility().getLinkId());
				first = false;
			}else{
				links.addAll(routeMe(networkForThisRoute, stops.get(stops.size() - 2), stop));
			}
		}
		
		NetworkRoute route = (NetworkRoute) networkForThisRoute.getFactory().createRoute(r.getTransportMode(), links.get(0), links.get(links.size()-1));
		route.setLinkIds(links.get(0), links.subList(1, links.size()-1), links.get(links.size()-1));
		return factory.createTransitRoute(r.getId(), route, stops, r.getTransportMode());
	}

	/**
	 * @param networkForThisRoute
	 * @param transitRouteStop
	 * @param stop
	 * @return
	 */
	private Collection<? extends Id> routeMe(NetworkImpl networkForThisRoute, TransitRouteStop start, TransitRouteStop end) {
		FreespeedTravelTimeCost cost = new FreespeedTravelTimeCost(-1, 0, 0);
		Dijkstra router = new Dijkstra(networkForThisRoute, cost, cost);
		
		Link startLink = networkForThisRoute.getLinks().get(start.getStopFacility().getLinkId());
		Link endLink = networkForThisRoute.getLinks().get(end.getStopFacility().getLinkId());
		List<Link> links = router.calcLeastCostPath(startLink.getToNode(), endLink.getToNode(), 0).links;
		Collection<Id> linkIds = new ArrayList<Id>();

		for(Link l: links){
			linkIds.add(l.getId());
		}
		return linkIds;
	}

	/**
	 * @param newSchedule
	 * @param s
	 * @return
	 */
	private TransitRouteStop createNewStop(TransitSchedule newSchedule, TransitRouteStop s, NetworkImpl networkForThisRoute) {
		TransitStopFacility fac =s.getStopFacility();
		Node nearest;
		if(newSchedule.getFacilities().containsKey(fac.getId())){
			fac = newSchedule.getFacilities().get(fac.getId());
		}else{
			fac = newSchedule.getFactory().createTransitStopFacility(fac.getId(), fac.getCoord(), fac.getIsBlockingLane());
			nearest = networkForThisRoute.getNearestNode(fac.getCoord());
			if(nearest.getInLinks().size() == 1){
				fac.setLinkId(nearest.getInLinks().keySet().iterator().next());
			}else if(nearest.getInLinks().size() < 1){
				log.error("no inLink");
			}else{
				log.error("to many inLinks");
			}
			newSchedule.addStopFacility(fac);
		}
		return newSchedule.getFactory().createTransitRouteStop(fac, s.getArrivalOffset(), s.getDepartureOffset());
	}

	private void createNetworkForThisLine(Network networkForThisRoute, TransitRoute r){
		addLink(networkForThisRoute, r.getRoute().getStartLinkId());
		addLink(networkForThisRoute, r.getRoute().getEndLinkId());
		for(Id linkId : r.getRoute().getLinkIds()){
			addLink(networkForThisRoute, linkId);
		}
		//remove DeadEnds
//		new NetworkCleaner().run(networkForThisRoute);
	}

	/**
	 * @param networkForThisRoute
	 * @param startLinkId
	 */
	private void addLink(Network networkForThisRoute, Id linkId) {
		Link link = this.osm.getNetwork().getLinks().get(linkId);
		Node start, end;
		start = link.getFromNode();
		end = link.getToNode();
		
		NetworkFactory factory = networkForThisRoute.getFactory();
		
		if(!networkForThisRoute.getNodes().containsKey(start.getId())){
			networkForThisRoute.addNode(factory.createNode(start.getId(), start.getCoord()));
		}
		if(!networkForThisRoute.getNodes().containsKey(end.getId())){
			networkForThisRoute.addNode(factory.createNode(end.getId(), end.getCoord()));
		}
		if(!networkForThisRoute.getLinks().containsKey(linkId)){
			networkForThisRoute.addLink(factory.createLink(link.getId(), 
					networkForThisRoute.getNodes().get(start.getId()), networkForThisRoute.getNodes().get(end.getId())));
		}
	}

}
