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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.PersonFilter;

/**
 * Created by amit on 31/12/2016.
 */

public class FilteredTripRouteDistanceHandler implements PersonDepartureEventHandler, LinkLeaveEventHandler,
        PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private static final Logger LOGGER = Logger.getLogger(FilteredTripRouteDistanceHandler.class);
    private final Vehicle2DriverEventHandler veh2DriverDelegate = new Vehicle2DriverEventHandler();

    private final TripRouteDistanceHandler delegate;

    private final PersonFilter pf ;
    private final String ug ;

    /**
     * User group filtering will be used, persons belongs to the given user group will be considered.
     */
    public FilteredTripRouteDistanceHandler(final Network network, final double simulationEndTime, final int noOfTimeBins, final String userGroup, final PersonFilter personFilter){
        this.delegate = new TripRouteDistanceHandler(network, simulationEndTime, noOfTimeBins);

        this.ug=userGroup;
        this.pf = personFilter;

        if( (this.ug==null && this.pf!=null) || this.ug!=null && this.pf==null ) {
            throw new RuntimeException("Either of person filter or user group is null.");
        } else if(this.ug!=null) {
            LOGGER.info("User group filtering is used, result will include all links but persons from "+this.ug+" user group only.");
        } else {
            LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
        }
    }

    /**
     * No filtering will be used, result will include all links, persons from all user groups.
     */
    public FilteredTripRouteDistanceHandler(final Network network, final double simulationEndTime, final int noOfTimeBins){
        this(network, simulationEndTime, noOfTimeBins, null,null);
    }

    public static void main(String[] args) {
        String dir = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/";
        String policyCase = "bau";

        EventsManager events = EventsUtils.createEventsManager();
        FilteredTripRouteDistanceHandler handler = new FilteredTripRouteDistanceHandler(
                LoadMyScenarios.loadScenarioFromNetwork(dir+policyCase+"/output_network.xml.gz").getNetwork(),
                36.*3600., 1,
                PatnaPersonFilter.PatnaUserGroup.urban.name(), new PatnaPersonFilter()
        );
        events.addHandler(handler);
        new MatsimEventsReader(events).readFile(dir+policyCase+"/output_events.xml.gz");

        SortedMap<String, Double> mode2dists = handler.getMode2TripDistace();

        BufferedWriter writer = IOUtils.getBufferedWriter(dir+policyCase+"/analysis/mode2dist_urban.txt");
        try {
            writer.write("mode \t distKm \n");
            for (String s : mode2dists.keySet()) {
                writer.write(s+"\t"+mode2dists.get(s)/1000.0 + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }


    }

    @Override
    public void reset(int iteration) {
        delegate.reset(iteration);
        veh2DriverDelegate.reset(iteration);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> driverId = veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
        if(this.ug==null || this.pf==null) {// no filtering
            delegate.handleEvent(event);
        } else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // user group filtering
            delegate.handleEvent(event);
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        delegate.handleEvent(event);
        veh2DriverDelegate.handleEvent(event);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        delegate.handleEvent(event);
        veh2DriverDelegate.handleEvent(event);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if(this.ug==null || this.pf==null) {// no filtering
            delegate.handleEvent(event);
        } else if (this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()).equals(this.ug)) { // user group filtering
            delegate.handleEvent(event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if(this.ug==null || this.pf==null) {// no filtering
            delegate.handleEvent(event);
        } else if (this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()).equals(this.ug)) { // user group filtering
            delegate.handleEvent(event);
        }
    }

    public SortedMap<Double, Map<Id<Person>, Integer>> getTimeBin2Person2TripsCount() {
        return delegate.getTimeBin2Person2TripsCount();
    }

    public SortedMap<Double, Map<Id<Person>, List<Double>>> getTimeBin2Person2TripsDistance() {
        return delegate.getTimeBin2Person2TripsDistance();
    }

    public SortedMap<String, Double> getMode2TripDistace(){
        return this.delegate.getMode2TripDistace();
    }

//    public Map<String, Double> getMode2TotalDistance(){
//        return getMode2PersonId2TotalTravelDistance().entrySet().stream().collect(
//                Collectors.toMap(
//                    entry -> entry.getKey(), entry -> MapUtils.doubleValueSum(entry.getValue())
//                )
//        );
//    }
}
