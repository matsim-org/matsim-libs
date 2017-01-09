/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.tripDistance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.*;

/**
 * @author amit
 */

public class TravelDistanceHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private static final Logger LOGGER = Logger.getLogger(TravelDistanceHandler.class.getName());

	private final Vehicle2DriverEventHandler veh2DriverDelegate = new Vehicle2DriverEventHandler();
	private final PersonFilter pf ;
	private final AreaFilter af;

	private final double timeBinSize ;
	private final Network network;
	private final String ug ;

	private final SortedMap<Double,Map<Id<Person>,Double>> timeBin2personId2travelledDistance = new TreeMap<>();

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public TravelDistanceHandler(final double simulationEndTime, final int noOfTimeBins, final Network network, final AreaFilter af, final PersonFilter pf, final String userGroup) {
		this.timeBinSize = simulationEndTime/noOfTimeBins;
		this.network = network;
		this.af = af;
		this.pf = pf;
		this.ug=userGroup;

		if( (this.ug==null && this.pf!=null) || this.ug!=null && this.pf==null ) {
			throw new RuntimeException("Either of person filter or user group is null.");
		} else if( this.ug!=null && this.af !=null) {
			LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the "+this.ug+" user group will be considered.");
		} else if(this.ug!=null) {
			LOGGER.info("User group filtering is used, result will include all links but persons from "+this.ug+" user group only.");
		} else if (this.af !=null) {
			LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
		} else {
			LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
		}
	}

	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public TravelDistanceHandler(final double simulationEndTime, final int noOfTimeBins, final Network network, final AreaFilter af) {
		this(simulationEndTime, noOfTimeBins, network, af, null,null);
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only.
	 */
	public TravelDistanceHandler(final double simulationEndTime, final int noOfTimeBins, final Network network, final PersonFilter pf, final String userGroup) {
		this(simulationEndTime, noOfTimeBins, network, null, pf, userGroup);
	}

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public TravelDistanceHandler(final double simulationEndTime, final int noOfTimeBins, final Network network) {
		this(simulationEndTime, noOfTimeBins, network,null, null, null);
	}

	public static void main(String[] args) {
		String scenario = "ei";
		String eventsFile = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/ITERS/it.1500/1500.events.xml.gz";
		String configFile = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/output_config.xml.gz";
		String networkFile = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/output_network.xml.gz";
		String outputFolder = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/hEART/output/"+scenario+"/analysis/";

		String shapeFileCity = FileUtils.SHARED_SVN+"/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		String shapeFileMMA = FileUtils.SHARED_SVN+"/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";

		String [] areas = {shapeFileCity, shapeFileMMA, null};
		String [] areasName = {"city","MMA","wholeArea"};

		double simEndTime = LoadMyScenarios.getSimulationEndTime(configFile);
		Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();

		SortedMap<String, SortedMap<String, Double>> area2usrGrp2Dist = new TreeMap<>();

		for(int ii=0; ii<areas.length;ii++) {
			AreaFilter areaFilter =  areas[ii] ==null ? null : new AreaFilter(areas[ii]);
			SortedMap<String, Double> usrGrp2Dist = new TreeMap<>();
			for ( MunichPersonFilter.MunichUserGroup ug : MunichPersonFilter.MunichUserGroup.values() ) {
				if(area2usrGrp2Dist.containsKey(ug.toString())) continue;

				EventsManager em = EventsUtils.createEventsManager();
				TravelDistanceHandler tdh = new TravelDistanceHandler(simEndTime, 1, network, areaFilter, new MunichPersonFilter(), ug.toString());
				em.addHandler(tdh);
				MatsimEventsReader reader = new MatsimEventsReader(em);
				reader.readFile(eventsFile);
				usrGrp2Dist.put(ug.toString(), MapUtils.doubleValueSum(tdh.getTimeBin2TravelDist() ) );
			}

			usrGrp2Dist.put("allPersons", MapUtils.doubleValueSum(usrGrp2Dist));
			area2usrGrp2Dist.put(areasName[ii], usrGrp2Dist);
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/areaToUserGroupToTotalTravelDistance.txt");
		try {
			writer.write("area \t userGroup \t totalTravelDistInKm \n");
			for(String a : area2usrGrp2Dist.keySet()) {
				for(String s : area2usrGrp2Dist.get(a).keySet()) {
					writer.write(a+"\t"+s+"\t"+area2usrGrp2Dist.get(a).get(s)/1000.0+"\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
	}

	@Override
	public void reset(int iteration) {
		this.veh2DriverDelegate.reset(iteration);
		this.timeBin2personId2travelledDistance.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		double time = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize;
		double dist = 0;
		Id<Person> driverId = veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
		Link link = network.getLinks().get(event.getLinkId());

		if (this.af!=null) { // area filtering
			if(! this.af.isLinkInsideShape(link)) return;

			if(this.ug==null || this.pf==null) {// only area filtering
				dist = link.getLength();
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // both filtering
				dist = link.getLength();
			}

		} else {
			if(this.ug==null || this.pf==null) {// no filtering
				dist = link.getLength();
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // user group filtering
				dist = link.getLength();
			}
		}

		if(timeBin2personId2travelledDistance.containsKey(time)) {
			Map<Id<Person>,Double> person2Dist = timeBin2personId2travelledDistance.get(time);
			if(person2Dist.containsKey(driverId)){
				person2Dist.put(driverId, person2Dist.get(driverId) + dist );
			} else {
				person2Dist.put(driverId, dist);	
			}
		} else {
			Map<Id<Person>,Double> person2Dist = new HashMap<>();
			person2Dist.put(driverId, dist);
			timeBin2personId2travelledDistance.put(time, person2Dist);
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.veh2DriverDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.veh2DriverDelegate.handleEvent(event);
	}

	/**
	 * @return time bin to person id to toll value
	 */
	public SortedMap<Double,Map<Id<Person>,Double>> getTimeBin2Person2TravelDist() {
		return timeBin2personId2travelledDistance;
	}

	/**
	 * @return timeBin to toll values for whole population
	 */
	public SortedMap<Double,Double> getTimeBin2TravelDist(){
		SortedMap<Double, Double> timebin2Dist = new TreeMap<>();
		for (double d :this.timeBin2personId2travelledDistance.keySet()){
			timebin2Dist.put(d, MapUtils.doubleValueSum(timeBin2personId2travelledDistance.get(d)));
		}
		return timebin2Dist;
	}
}