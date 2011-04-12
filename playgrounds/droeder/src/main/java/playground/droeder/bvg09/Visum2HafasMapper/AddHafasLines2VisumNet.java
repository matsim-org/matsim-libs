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

package playground.droeder.bvg09.Visum2HafasMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

public class AddHafasLines2VisumNet {

	private final static Logger log = Logger.getLogger(AddHafasLines2VisumNet.class);
	private final String PATH = DaPaths.OUTPUT + "bvg09/";

	private final String NETFILE = PATH + "intermediateNetwork.xml";
	private final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
	private final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
	private final String FINALTRANSITFILE = PATH + "finalTransit.xml";
	private final String NEWLINESSHAPE = PATH + "newLines.shp";

	private final Id ERROR = new IdImpl("Error");

	private ScenarioImpl visumSc;
	private ScenarioImpl hafasSc;
	private ScenarioImpl newSc;

	private NetworkImpl visumNet;
	private TransitSchedule visumTransit;

	private TransitSchedule hafasTransit;

	private TransitSchedule finalTransitSchedule;
	private TransitScheduleFactory finalTransitFactory;


	private TreeMap<Id, Id> vis2HafLines;

	private Map<Id, Id> haf2VisNearestStop;

	public static void main (String[] args){
		AddHafasLines2VisumNet add = new AddHafasLines2VisumNet();
		add.run();
	}


	public AddHafasLines2VisumNet(){
		this.visumSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.hafasSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUMTRANSITFILE, visumSc);
		visumTransit = visumSc.getTransitSchedule();
		new MatsimNetworkReader(visumSc).readFile(NETFILE);
		visumNet = visumSc.getNetwork();

		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFASTRANSITFILE, hafasSc);
		hafasTransit = hafasSc.getTransitSchedule();

		this.createHafasLineIdsFromVisum();

		newSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		newSc.getConfig().scenario().setUseTransit(true);
		finalTransitFactory = newSc.getTransitSchedule().getFactory();
		finalTransitSchedule = finalTransitFactory.createTransitSchedule();
	}

	public void run(){
		this.createHafasLineIdsFromVisum();
		this.findNearestStops();
		this.getNewRoutesFromMatchedStops();
		this.validate();
//		DaShapeWriter.writeDefaultLineString2Shape(NEWLINESSHAPE, "newRoutes", prepareNewRoutesForShape(), null);
	}

	private void readSchedule(String fileName, ScenarioImpl sc){
		TransitScheduleReader reader = new TransitScheduleReader(sc);
		try {
			reader.readFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void findNearestStops(){
		this.haf2VisNearestStop = new HashMap<Id, Id>();

		for(TransitStopFacility h : hafasTransit.getFacilities().values()){
			Id vis = ERROR;
			Double dist = Double.MAX_VALUE;
			for(TransitStopFacility v : visumTransit.getFacilities().values()){
				double temp = getDist(v.getCoord(), h.getCoord());
				if(temp < dist){
					dist = temp;
					vis = v.getId();
				}
			}
			if(vis.equals(ERROR)){
				throw new RuntimeException("no Stop matched to hafasstop " + h.getId() + "!");
			}else{

				haf2VisNearestStop.put(h.getId(), vis);
			}
		}

		// add some points not matched by the algorithm
		haf2VisNearestStop.put(new IdImpl("9160525"), new IdImpl("1605250"));
		haf2VisNearestStop.put(new IdImpl("9160524"), new IdImpl("1605240"));
		haf2VisNearestStop.put(new IdImpl("9160540"), new IdImpl("1605400"));
		haf2VisNearestStop.put(new IdImpl("9160539"), new IdImpl("1605390"));
		haf2VisNearestStop.put(new IdImpl("9160011"), new IdImpl("1600110"));
		haf2VisNearestStop.put(new IdImpl("9160523"), new IdImpl("1605230"));
		haf2VisNearestStop.put(new IdImpl("9160021"), new IdImpl("1600210"));
	}

	private Map<String, SortedMap<Integer, Coord>> prepareNewRoutesForShape(){
		Map<String, SortedMap<Integer, Coord>> preparedRoutes = new HashMap<String, SortedMap<Integer,Coord>>();

		for(TransitLine l : finalTransitSchedule.getTransitLines().values()){
			for(TransitRoute r : l.getRoutes().values()){
				int  i = 0;
				SortedMap<Integer, Coord> temp = new TreeMap<Integer, Coord>();
				for(Id id : r.getRoute().getLinkIds()){
					temp.put(i, visumNet.getLinks().get(id).getFromNode().getCoord());
					i++;
					temp.put(i, visumNet.getLinks().get(id).getToNode().getCoord());
					i++;
				}
				preparedRoutes.put(l.getId().toString() + "_" + r.getId().toString(), temp);
			}

		}
		return preparedRoutes;
	}

	private void getNewRoutesFromMatchedStops(){
		TransitLine newLine;
		TransitRoute newRoute;
		TransitRouteStop newStop;
		TransitStopFacility newFacility;
		List<TransitRouteStop> stops;


		Map<Id, TransitStopFacility> facilities = new HashMap<Id, TransitStopFacility>();
		for (Entry<Id, Id> e : vis2HafLines.entrySet()){
			newLine = finalTransitFactory.createTransitLine(e.getKey());
			for(TransitRoute hr : hafasTransit.getTransitLines().get(e.getValue()).getRoutes().values()){
				stops = new ArrayList<TransitRouteStop>();

				for(TransitRouteStop stop : hr.getStops()){
					TransitStopFacility hFacility = stop.getStopFacility();
					TransitStopFacility vFacility = visumTransit.getFacilities().get(haf2VisNearestStop.get(hFacility.getId()));

					if(facilities.containsKey(vFacility.getId())){
						newFacility = facilities.get(vFacility.getId());
					}else{
						newFacility = finalTransitFactory.createTransitStopFacility(vFacility.getId(), vFacility.getCoord(),vFacility.getIsBlockingLane());
						newFacility.setLinkId(findNextLink(newFacility));
						facilities.put(vFacility.getId(), newFacility);
					}

					newStop = finalTransitFactory.createTransitRouteStop(newFacility, stop.getArrivalOffset(), stop.getDepartureOffset());
					stops.add(newStop);
				}

				NetworkRoute networkRoute = getNetworkRoute(stops);

				newRoute = finalTransitFactory.createTransitRoute(hr.getId(), networkRoute, stops, TransportMode.pt);
				newLine.addRoute(newRoute);
			}
			finalTransitSchedule.addTransitLine(newLine);
		}
	}

	private NetworkRoute getNetworkRoute(List<TransitRouteStop> stops){
		List<Link> links = new ArrayList<Link>();

		LeastCostPathCalculator router = new Dijkstra(visumNet,
				new FreespeedTravelTimeCost(new PlanCalcScoreConfigGroup()),
				new FreespeedTravelTimeCost(new PlanCalcScoreConfigGroup()));

		boolean first = true;
		Path p;
		Node fromNode = null;
		Node toNode;
		for(TransitRouteStop stop: stops){
			toNode = visumNet.getLinks().get(stop.getStopFacility().getLinkId()).getToNode();
			if(!first){
				p = router.calcLeastCostPath(fromNode, toNode, 0);
				links.addAll(p.links);
			}
			fromNode = toNode;
			first = false;
		}

		List<Id> linkIds = NetworkUtils.getLinkIds(links);
		Id start, end;
		start = stops.get(0).getStopFacility().getLinkId();
		end = stops.get(stops.size()-1).getStopFacility().getLinkId();
		NetworkRoute route = new LinkNetworkRouteImpl(start, end);
		route.setLinkIds(start, linkIds, end);
		return route;
	}



	private Id findNextLink(TransitStopFacility newFacility) {
		if(newFacility.getId().equals(new IdImpl("484014"))){
			return new IdImpl("1397");
		} else if(newFacility.getId().equals(new IdImpl("484013"))){
			return new IdImpl("2170");
		}else{
			Double dist = Double.MAX_VALUE;
			Node n;
			Id link = null;
			for(Link l : visumNet.getLinks().values()){
				n = l.getToNode();
				double temp = getDist(newFacility.getCoord(), n.getCoord());
				if (temp<dist){
					dist = temp;
					link = l.getId();
				}
			}
			return link;
		}
	}


	private void createHafasLineIdsFromVisum(){
		vis2HafLines = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
			createdHafasId = new StringBuffer();
			idToChar = line.getId().toString().split("");

			if(idToChar[1].equals("B")){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("U")){
				createdHafasId.append(idToChar[1]);
				createdHafasId.append(idToChar[3]);
				createdHafasId.append("   ");
			}else if(idToChar[1].equals("T") && idToChar[3].equals("M") ){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("T") && !(idToChar.equals("M")) ){
				createdHafasId.append(idToChar[3]);
				createdHafasId.append(idToChar[4]);
				createdHafasId.append("   ");
			}

			hafasId = createdHafasId.toString();
			if(createdHafasId.length()>0 && hafasSc.getTransitSchedule().getTransitLines().containsKey(new IdImpl(hafasId)) ){
				vis2HafLines.put(line.getId() , new IdImpl(hafasId));
			}
		}
	}

	private void validate(){

		for(TransitLine l : finalTransitSchedule.getTransitLines().values()){
			for(TransitRoute r : l.getRoutes().values()){
				double offset = 0;
				for(TransitRouteStop s : r.getStops()){
					if(s.getArrivalOffset()< offset){
						log.error("On Line:" + l.getId()+ " route:" + r.getId() +
								" Arrivaloffset of Stop " + s.getStopFacility().getId() + " then departureOffset of the stop before!");
					}
					offset = s.getDepartureOffset();
				}
			}
		}
	}

	protected double getDist(Coord one, Coord two){

		double xDif = one.getX() - two.getX();
		double yDif = one.getY() - two.getY();

		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
	}
}
