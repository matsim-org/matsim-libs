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

package playground.agarwalamit.analysis.legMode.distributions;

import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Created by amit on 20/10/16.
 */

public class LegModeBeelineDistanceDistributionHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    private static final Logger LOG = Logger.getLogger(LegModeBeelineDistanceDistributionHandler.class);

    private SortedMap<String, SortedMap<Double, Integer>> mode2DistClass2Count = new TreeMap<>();
    private Map<Id<Person>,Coord> personId2DepartCoord = new HashMap<>();
    private List<Double> distClasses;
    private Network network;

    public LegModeBeelineDistanceDistributionHandler (final Network network) {
        Double [] dists = new Double[] {0., 2000., 4000., 6000., 8000., 10000.};
        this.distClasses = Arrays.asList(dists);
        this.network = network;
    }

    public LegModeBeelineDistanceDistributionHandler(final List<Double> distClasses, final Network network) {
        this.distClasses = distClasses;
        this.network = network;
        LOG.info("Assuming the distances in m.");
    }

    @Override
    public void reset(int iteration) {
        this.mode2DistClass2Count.clear();
        this.personId2DepartCoord.clear();
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if(personId2DepartCoord.containsKey(event.getPersonId())) throw new RuntimeException("Person should not be in the map. Event :"+ event.toString());
        else {
            Coord cord = this.network.getLinks().get(event.getLinkId()).getCoord();
            personId2DepartCoord.put(event.getPersonId(), cord);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Coord fromCoord = this.personId2DepartCoord.remove(event.getPersonId());
        Coord toCoord = this.network.getLinks().get(event.getLinkId()).getCoord();
        double dist = CoordUtils.calcEuclideanDistance(fromCoord,toCoord);
        double distClass = getDistanceBin(dist);

        // store info
        String mode = event.getLegMode();
        SortedMap<Double, Integer> dist2counts = this.mode2DistClass2Count.get(mode);
        if( dist2counts == null ) {
            dist2counts = new TreeMap<>();
            dist2counts.put(distClass, 1);
            this.mode2DistClass2Count.put(mode,dist2counts);
        } else {
            if(dist2counts.containsKey(distClass)) dist2counts.put(distClass, dist2counts.get(distClass)+1);
            else dist2counts.put(distClass, 1);
        }
    }

    private double getDistanceBin(final double dist){ // dist = 1500
        for( int i = 0; i < this.distClasses.size() ; i++   ) {
            if (i == this.distClasses.size()-1 ) return this.distClasses.get(i);
            else if (dist >= this.distClasses.get(i) && dist < this.distClasses.get(i+1) ) {
                return this.distClasses.get(i);
            }
        }
        throw new RuntimeException("Dist class for distance "+ dist+ "is not found.");
    }

    public SortedMap<String, SortedMap<Double, Integer>> getMode2DistanceClass2LegCounts(){
        return this.mode2DistClass2Count;
    }
}