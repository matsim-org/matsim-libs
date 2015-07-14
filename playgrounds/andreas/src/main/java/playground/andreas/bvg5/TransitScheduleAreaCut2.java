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
package playground.andreas.bvg5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.BVGLines2PtModes;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.PtMode2LineSetter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.opengis.feature.simple.SimpleFeature;

import playground.andreas.utils.pt.transitSchedule2shape.DaShapeWriter;
import playground.andreas.utils.pt.transitSchedule2shape.TransitSchedule2Shape;
import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.transitSchedule2Shp.TransitSchedule2Shp;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class TransitScheduleAreaCut2 {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitScheduleAreaCut2.class);
	private Geometry area;
	private TransitSchedule schedule;
	private PtMode2LineSetter ptMode2LineSetter;
	private Set<String> modes2Cut;
	private TransitSchedule newSchedule;
	private Vehicles vehicles;
	private Vehicles newVehicles;
	private int vehicleCnt = 0;

	/**
	 * 
	 * @param transitSchedule, the transitschedule to cut
	 * @param areaShape, shapefile that contains the area 2 cut
	 * @param ptMode2LineSetter, only lines will be cutted that are set here and those mode is set in modes2cut 
	 * @param modes2Cut,
	 * @param vehicles, the old vehicles
	 */
	public TransitScheduleAreaCut2(TransitSchedule transitSchedule, String areaShape, PtMode2LineSetter ptMode2LineSetter, Set<String> modes2Cut, Vehicles vehicles) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(areaShape);
		this.ptMode2LineSetter = ptMode2LineSetter;
		this.modes2Cut = modes2Cut;
		for (SimpleFeature simpleFeature : features) {
			if (this.area == null) {
				this.area = (Geometry) simpleFeature.getDefaultGeometry();
			} else {
				this.area = this.area.union((Geometry) simpleFeature.getDefaultGeometry());
			}
		}
		this.schedule = transitSchedule;
		this.vehicles = vehicles;
	}
	
	/**
	 * 
	 * @param transitSchedule, the transitschedule to cut
	 * @param area2Cut, the area to cut
	 * @param ptMode2LineSetter, only lines will be cutted that are set here and those mode is set in modes2cut 
	 * @param modes2Cut,
	 * @param vehicles, the old vehicles
	 */
	public TransitScheduleAreaCut2(TransitSchedule transitSchedule, Geometry area2Cut, PtMode2LineSetter ptMode2LineSetter, Set<String> modes2Cut, Vehicles vehicles){
		this.ptMode2LineSetter = ptMode2LineSetter;
		this.modes2Cut = modes2Cut;
		this.area = area2Cut;
		this.schedule = transitSchedule;
		this.vehicles = vehicles;
	}
	
	/**
	 * will create output at outdir, namely cuttedSchedule.xml.gz and cuttedScheduleVehicles.xml.gz
	 * @param outdir, might be null. Then no output is written, but the data may be accessed with according getters...
	 */
	public void run(String outdir){
		TransitLine line;
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		this.newSchedule = sc.getTransitSchedule();
		this.newVehicles = ((ScenarioImpl) sc).getTransitVehicles();
		TransitScheduleFactory factory = this.newSchedule.getFactory();
		
		//copy the vehicles
		for ( VehicleType t : this.vehicles.getVehicleTypes().values() ) {
			this.newVehicles.addVehicleType( t );
		}
		for ( Vehicle v : this.vehicles.getVehicles().values() ) {
			this.newVehicles.addVehicle( v );
		}
		//add all TransitStopFacilities to the new schedule
		for(TransitStopFacility facility : this.schedule.getFacilities().values()){
			this.newSchedule.addStopFacility(facility);
		}
		// handle the transitLines
		for(TransitLine oldLine :  this.schedule.getTransitLines().values()){
			// don't know what to do with this line, just add it again to the schedule
			if(!this.ptMode2LineSetter.getLineId2ptModeMap().containsKey(oldLine.getId())){
				this.newSchedule.addTransitLine(oldLine);
				continue;
			}
			// don't cut this line, but add it to the new schedule
			if(!this.modes2Cut.contains(this.ptMode2LineSetter.getLineId2ptModeMap().get(oldLine.getId()))){
				this.newSchedule.addTransitLine(oldLine);
				continue;
			}
			//create a new transitline
			line = factory.createTransitLine(oldLine.getId());
			// do what ever we want to do with these routes and add the new routes to the new line
			for(TransitRoute newRoute: handleRoutes(factory,  oldLine.getRoutes().values())){
				line.addRoute(newRoute);
			}
			// add the new line to the schedule,
			// but only when at least one route exists. It might happen that a route is completely covered by 
			// area to cut. Thus, it would result in an empty line
			if(line.getRoutes().isEmpty()){
				log.warn("line " + oldLine.getId().toString() + " seems to be covered completely by the ``area to cut''. Thus, the line is completely deleted..."); 
			}else{
				this.newSchedule.addTransitLine(line);
			}
		}
		
		if(!(outdir == null)){
			new TransitScheduleWriter(this.newSchedule).writeFile(outdir + "cuttedSchedule.xml.gz");
			new VehicleWriterV1(this.newVehicles).writeFile(outdir + "cuttedScheduleVehicles.xml.gz");
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public TransitSchedule getNewSchedule(){
		return this.newSchedule;
	}
	
	
	/**
	 * @return
	 */
	private Vehicles getNewVehicles() {
		return this.newVehicles;
	}


	/**
	 * @param factory
	 * @param routes
	 * @return
	 */
	private List<TransitRoute> handleRoutes(TransitScheduleFactory factory, Collection<TransitRoute> routes) {
		List<TransitRoute> newRoutes = new ArrayList<TransitRoute>();
		//handle all routes
		for(TransitRoute oldRoute: routes){
			//what to do with the route
			if(routeAffectsArea(oldRoute)){
				// it affects the area, do something
				newRoutes.addAll(handleRoute(oldRoute, factory));
			}else{
				// it is somewhere else. just add it
				newRoutes.add(oldRoute);
			}
		}
		return newRoutes;
	}

	/**
	 * @param oldRoute
	 * @param factory
	 * @return
	 */
	private Collection<? extends TransitRoute> handleRoute(TransitRoute oldRoute,	TransitScheduleFactory factory) {
		List<List<TransitRouteStop>> routeFragments = getRouteFragments(oldRoute);
		return createResultingRoutes(routeFragments, oldRoute, factory);
	}



	/**
	 * @param routeFragments
	 * @param oldRoute
	 * @param factory
	 * @return
	 */
	private Collection<? extends TransitRoute> createResultingRoutes(List<List<TransitRouteStop>> routeFragments,
			TransitRoute oldRoute, TransitScheduleFactory factory) {
		int routecnt = 0;
		List<TransitRoute> newRoutes = new ArrayList<TransitRoute>();
		//copy the old Routes link sequence
		List<Id<Link>> oldLinkIds = new ArrayList<>();
		oldLinkIds.add(oldRoute.getRoute().getStartLinkId());
		oldLinkIds.addAll(oldRoute.getRoute().getLinkIds());
		oldLinkIds.add(oldRoute.getRoute().getEndLinkId());
		ListIterator<Id<Link>> linkIdIterator = oldLinkIds.listIterator();
		// ####
		
		TransitRoute transitRoute;
		NetworkRoute networkRoute;
		List<TransitRouteStop> stops;
		Id<Link> startLinkId, endLinkId;
		List<Id<Link>> linkIds;
		Id<Link> tempId;
		Double initialDepartureOffset = oldRoute.getStops().get(0).getDepartureOffset();
		Double departureOffset;
		
		// handle the resulting route fragments
		for(List<TransitRouteStop> s: routeFragments){
			// calculate the departure offset for this fragment
			departureOffset = s.get(0).getDepartureOffset() - initialDepartureOffset;
			// create the links for the network route
			linkIds = new ArrayList<Id<Link>>();
			startLinkId = s.get(0).getStopFacility().getLinkId();
			endLinkId = s.get(s.size() -1).getStopFacility().getLinkId();
			// add all links between start and end
			boolean start = false;
			while(linkIdIterator.hasNext()){
				tempId = linkIdIterator.next();
				if(!start){
					// start the sequence with the first link after the start link
					if(tempId.equals(startLinkId)) start = true;
					continue;
				}
				if(tempId.equals(endLinkId)) {
					// end of this route part, skip the end link
					break;
				}
				// it is neither the start nor the end link, thus we store it...
				linkIds.add(tempId);
			}
			// create the network Route
			networkRoute = new LinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
			stops = new ArrayList<TransitRouteStop>();
			// copy and process the necessary stops. Thus, keep the, the facilities and awaitDeparture but shift the departureOffsets
			stops.addAll(copyStops(s, factory, departureOffset));
			// create the new departures
			transitRoute = factory.createTransitRoute(Id.create(oldRoute.getId().toString() + "_" + routecnt, TransitRoute.class), 
						networkRoute, stops, oldRoute.getTransportMode());
			// copy and shift the departures according to the calculated offset
			for(Departure departure : copyDepartures(oldRoute.getDepartures(), factory, departureOffset)){
				transitRoute.addDeparture(departure);
			}
			// now we have a new and complete transitRoute
			newRoutes.add(transitRoute);
			routecnt ++;
		}
		return newRoutes;
	}


	/**
	 * @param departures
	 * @param factory
	 * @param departureOffset
	 * @return
	 */
	private List<Departure> copyDepartures(Map<Id<Departure>, Departure> departures, TransitScheduleFactory factory, double departureOffset) {
		List<Departure> newDepartures = new ArrayList<Departure>();
		for(Departure dep: departures.values()){
			// create a new vehicle of the same type
			Vehicle v = newVehicles.getFactory().createVehicle(Id.create("cutOff_" + vehicleCnt, Vehicle.class), this.vehicles.getVehicles().get(dep.getVehicleId()).getType());
			// copy the departures but shift the departures
			Departure newDep = factory.createDeparture(Id.create(this.vehicleCnt, Departure.class), dep.getDepartureTime() + departureOffset);
			// set the vehicle for the departure
			newDep.setVehicleId(v.getId());
			// store the new vehicle
			this.newVehicles.addVehicle( v);
			// and the new departure
			newDepartures.add(newDep);
			this.vehicleCnt++;
		}
		return newDepartures;
	}


	/**
	 * @param s
	 * @param factory
	 * @return
	 */
	private Collection<? extends TransitRouteStop> copyStops(List<TransitRouteStop> s, TransitScheduleFactory factory, Double departureOffset) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitRouteStop newStop;
		for(TransitRouteStop oldStop: s){
			//shift the departures, but nothing else... set arrival and departureOffset identical as the arrival offset has no influence and is not compulsory...
			newStop = factory.createTransitRouteStop(oldStop.getStopFacility(), 
					oldStop.getDepartureOffset() - departureOffset, 
					oldStop.getDepartureOffset() - departureOffset);
			newStop.setAwaitDepartureTime(oldStop.isAwaitDepartureTime());
			stops.add(newStop);
		}
		return stops;
	}


	/**
	 * @param oldRoute
	 * @return
	 */
	private List<List<TransitRouteStop>> getRouteFragments(TransitRoute oldRoute) {
		List<List<TransitRouteStop>> fragments = new ArrayList<List<TransitRouteStop>>();
		List<TransitRouteStop> temp;
		
		temp = new ArrayList<TransitRouteStop>();
		for(int i = 0; i < oldRoute.getStops().size(); i++){
			if(temp.size() == 0){
				// irrespective of the location of the stop, we keep it here
				temp.add(oldRoute.getStops().get(i));
			}else{
				TransitRouteStop stop = oldRoute.getStops().get(i);
				if(stopAffectsArea(temp.get(temp.size()-1))){
//					previous stop affects area
					if(stopAffectsArea(stop)){
						// this stop affects the area as well, cut here
						if(temp.size() > 1){
							fragments.add(temp);
						}
						temp = new ArrayList<TransitRouteStop>();
						// initialize a new fragment
						temp.add(stop);
					}else{
						// this stop does not affect the area, add...
						temp.add(stop);
					}
				}else{
					// the previous stop does not affect the area, thus we keep the new one...
					temp.add(stop);
				}
			}
		}
		if(temp.size() > 1){
			fragments.add(temp);
		}
		return fragments;
	}

	/**
	 * @param stop
	 * @return
	 */
	private boolean stopAffectsArea(TransitRouteStop stop) {
		if(this.area.contains(MGC.coord2Point(stop.getStopFacility().getCoord()))){
			//at least one stop is in the area of interest
			return true;
		}
		return false;
	}

	/**
	 * @param oldRoute
	 * @return
	 */
	private boolean routeAffectsArea(TransitRoute oldRoute) {
		// check if the route affects the area of interest
		for(TransitRouteStop stop: oldRoute.getStops()){
			if(stopAffectsArea(stop)){
				//at least one stop is in the area of interest
				return true;
			}
		}
		return false;
	}
	
	
	public static void main(String[] args) {
		String dir ="C:/Users/Daniel/Desktop/para/";
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(sc).readFile(dir + "remainingSchedule.xml.gz");
		new VehicleReaderV1(((ScenarioImpl) sc).getTransitVehicles() ).readFile(dir + "transitVehicles100.final.xml.gz");
		
		BVGLines2PtModes lines2mode = new BVGLines2PtModes();
		lines2mode.setPtModesForEachLine(sc.getTransitSchedule(), "p");
		Set<String> modes2Cut = new HashSet<String>(){{
			add("bvg_bus");
		}};
		
		DaShapeWriter.writeTransitLines2Shape(dir + "oldSchedule.shp", sc.getTransitSchedule(), null, TransitSchedule2Shape.getAttributesForLines(sc.getTransitSchedule(), "p"), TransformationFactory.WGS84_UTM33N);
		TransitScheduleAreaCut2 areacut = new TransitScheduleAreaCut2(sc.getTransitSchedule(), dir + "scenarioArea.shp", lines2mode, modes2Cut, ((ScenarioImpl)sc).getTransitVehicles());
		areacut.run(dir);
		DaShapeWriter.writeTransitLines2Shape(dir + "cuttedSchedule.shp", areacut.getNewSchedule(), null, TransitSchedule2Shape.getAttributesForLines(areacut.getNewSchedule(), "p"), TransformationFactory.WGS84_UTM33N);

		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc2.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(sc2).readFile(dir + "cuttedSchedule.xml.gz");
		new MatsimNetworkReader(sc2).readFile(dir + "network.final.xml.gz");
		
		
		VspAnalyzer analyzer = new VspAnalyzer(dir);
		analyzer.addAnalysisModule(new TransitSchedule2Shp(sc2, TransformationFactory.WGS84_UTM33N));
		analyzer.run();
		
	}
	

	
//	public static void main(String[] args) {
//		
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		sc.getConfig().transit().setUseTransit(true);
//		sc.getConfig().scenario().setUseVehicles(true);
//		
//		VehicleType type = ((ScenarioImpl) sc).getVehicles().getFactory().createVehicleType(sc.createId("bus"));
//		Vehicle v = ((ScenarioImpl) sc).getVehicles().getFactory().createVehicle(sc.createId("bus1"), type);
//		((ScenarioImpl) sc).getVehicles().addVehicle( v);
//		((ScenarioImpl) sc).getVehicles().getVehicleTypes().put(type.getId(), type);
//		
//		TransitSchedule schedule = sc.getTransitSchedule();
//		TransitScheduleFactory f = schedule.getFactory();
//		TransitStopFacility facility = f.createTransitStopFacility(sc.createId("1"), sc.createCoord(1, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility = f.createTransitStopFacility(sc.createId("2"), sc.createCoord(2, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility = f.createTransitStopFacility(sc.createId("3"), sc.createCoord(3, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility = f.createTransitStopFacility(sc.createId("4"), sc.createCoord(4, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("5"), sc.createCoord(5, 2), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("6"), sc.createCoord(6, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("7"), sc.createCoord(7, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("8"), sc.createCoord(8, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("9"), sc.createCoord(9, 2), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("10"), sc.createCoord(10, 2), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("11"), sc.createCoord(11, 2), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("12"), sc.createCoord(12, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("13"), sc.createCoord(13, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility =f.createTransitStopFacility(sc.createId("14"), sc.createCoord(13, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		facility = f.createTransitStopFacility(sc.createId("15"), sc.createCoord(13, 1), false);
//		facility.setLinkId(facility.getId());
//		schedule.addStopFacility(facility);
//		
//		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("1")), 0, 1));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("2")), 2, 3));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("3")), 4, 5));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("4")), 6, 7));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("5")), 8, 9));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("6")), 10, 11));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("7")), 12, 13));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("8")), 14, 15));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("9")), 16, 17));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("10")), 18, 19));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("11")), 20, 21));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("12")), 22, 23));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("13")), 24, 25));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("14")), 26, 27));
//		stops.add(f.createTransitRouteStop(schedule.getFacilities().get(Id.create("15")), 28, 29));
//		
//		List<Id> links = new ArrayList<Id>();
//		links.add(Id.create("2"));
//		links.add(Id.create("3"));
//		links.add(Id.create("4"));
//		links.add(Id.create("5"));
//		links.add(Id.create("6"));
//		links.add(Id.create("5"));
//		links.add(Id.create("6"));
//		links.add(Id.create("7"));
//		links.add(Id.create("8"));
//		links.add(Id.create("9"));
//		links.add(Id.create("10"));
//		links.add(Id.create("11"));
//		links.add(Id.create("12"));
//		links.add(Id.create("13"));
//		links.add(Id.create("14"));
//		NetworkRoute netRoute = new LinkNetworkRouteImpl(Id.create("1"), links, Id.create("15"));
//		TransitRoute route = f.createTransitRoute(sc.createId("r1"), netRoute, stops, TransportMode.pt);
//		Departure dep = f.createDeparture(sc.createId("d1"), 3600);
//		dep.setVehicleId(Id.create("bus1"));
//		route.addDeparture(dep);
//		TransitLine line = f.createTransitLine(sc.createId("-B-1"));
//		line.addRoute(route);
//		schedule.addTransitLine(line);
//		
//		BVGLines2PtModes line2Mode = new BVGLines2PtModes();
//		line2Mode.setPtModesForEachLine(schedule, "p");
//		
//		Set<String> modes2cut = new HashSet<String>();
//		modes2cut.add("bvg_bus");
//		
//		GeometryFactory geoFac = new GeometryFactory();
//		Coordinate[] coords;
//		Geometry area2Cut;
//		TransitScheduleAreaCut2 areacut;
//		TransitSchedule newSchedule;
//		
//		
//		//TEST 1
//		coords = new Coordinate[4];
//		coords[0] = new Coordinate(0, 0);
//		coords[1] = new Coordinate(3.5, 0);
//		coords[2] = new Coordinate(3.5, 1.5);
//		coords[3] = new Coordinate(0, 1.5);
//		area2Cut = geoFac.createMultiPoint(coords).convexHull();
//		areacut = new TransitScheduleAreaCut2(schedule, area2Cut, line2Mode, modes2cut, ((ScenarioImpl) sc).getVehicles());
//		areacut.run(null);
//		test(areacut.getNewSchedule(), route, areacut.getNewVehicles());
//
//		// TEST2
//		coords = new Coordinate[4];
//		coords[0] = new Coordinate(3.5, 0);
//		coords[1] = new Coordinate(12.5, 0);
//		coords[2] = new Coordinate(12.5, 1.5);
//		coords[3] = new Coordinate(3.5, 1.5);
//		area2Cut = geoFac.createMultiPoint(coords).convexHull();
//		areacut = new TransitScheduleAreaCut2(schedule, area2Cut, line2Mode, modes2cut, ((ScenarioImpl) sc).getVehicles());
//		areacut.run(null);
//		test(areacut.getNewSchedule(), route, areacut.getNewVehicles());
//		
//		// TEST 3
//		coords = new Coordinate[4];
//		coords[0] = new Coordinate(11.5, 0);
//		coords[1] = new Coordinate(15.5, 0);
//		coords[2] = new Coordinate(15.5, 1.5);
//		coords[3] = new Coordinate(11.5, 1.5);
//		area2Cut = geoFac.createMultiPoint(coords).convexHull();
//		areacut = new TransitScheduleAreaCut2(schedule, area2Cut, line2Mode, modes2cut, ((ScenarioImpl) sc).getVehicles());
//		areacut.run(null);
//		test(areacut.getNewSchedule(), route, areacut.getNewVehicles());
//		
//	}


	private static void test(TransitSchedule newSchedule, TransitRoute oldRoute, Vehicles newVehicles){
		System.out.println();
		
		System.out.print("oldSchedule:\n");
		for(Departure d: oldRoute.getDepartures().values()){
			System.out.println("routeId: " + oldRoute.getId().toString() + "time: " + d.getDepartureTime() + 
					"\t vehId: " + d.getVehicleId() + 
					"\tvehType: " + newVehicles.getVehicles().get(d.getVehicleId()).getType().getId().toString());
		}
		System.out.print("stops:\t\t");
		for(TransitRouteStop stop: oldRoute.getStops()){
			System.out.print(stop.getStopFacility().getId().toString() +"\t");
		}
		System.out.print("\nDepOffSet:\t");
		for(TransitRouteStop stop: oldRoute.getStops()){
			System.out.print(stop.getDepartureOffset() +"\t");
		}
		System.out.println();
		System.out.print("links:\t\t" +oldRoute.getRoute().getStartLinkId() + "\t");
		for(Id linkId: oldRoute.getRoute().getLinkIds()){
			System.out.print(linkId +"\t");
		}
		System.out.println(oldRoute.getRoute().getEndLinkId() +" \n");
		System.out.println("\n");
		System.out.println("new Schedule");
//		System.out.println("lines: " + newSchedule.getTransitLines().size() + "\troutes: " + newSchedule.getTransitLines().get(Id.create("-B-1")).getRoutes().size());
		for(TransitRoute r: newSchedule.getTransitLines().get(Id.create("-B-1", TransitLine.class)).getRoutes().values()){
			for(Departure d: r.getDepartures().values()){
				System.out.println("routeId: " + r.getId().toString() + "\ttime: " + d.getDepartureTime() + 
						"\t vehId: " + d.getVehicleId() + 
						"\tvehType: " + newVehicles.getVehicles().get(d.getVehicleId()).getType().getId().toString());
			}
			System.out.print("stops:\t\t");
			for(TransitRouteStop stop: r.getStops()){
				System.out.print(stop.getStopFacility().getId().toString() +"\t");
			}
			System.out.print("\nDepOffSet:\t");
			for(TransitRouteStop stop: r.getStops()){
				System.out.print(stop.getDepartureOffset() +"\t");
			}
			System.out.println();
			System.out.print("links:\t\t" +r.getRoute().getStartLinkId() + "\t");
			for(Id linkId: r.getRoute().getLinkIds()){
				System.out.print(linkId +"\t");
			}
			System.out.println(r.getRoute().getEndLinkId() +" \n");
		}
		System.out.println("\n\n");
	}
}

