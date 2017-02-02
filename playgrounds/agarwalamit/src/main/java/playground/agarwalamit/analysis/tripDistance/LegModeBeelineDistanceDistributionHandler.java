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

    private final SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2distances = new TreeMap<>();

    private final SortedMap<String, SortedMap<Double, Integer>> mode2DistClass2Count = new TreeMap<>();
    private final Map<Id<Person>,Coord> personId2DepartCoord = new HashMap<>();
    private final List<Double> distClasses;
    private final Network network;

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
        Id<Person> personId = event.getPersonId();
        Coord fromCoord = this.personId2DepartCoord.remove(personId);
        Coord toCoord = this.network.getLinks().get(event.getLinkId()).getCoord();
        double dist = CoordUtils.calcEuclideanDistance(fromCoord,toCoord);
        double distClass = getDistanceBin(dist);

        // store info
        String mode = event.getLegMode();
        SortedMap<Double, Integer> dist2counts = this.mode2DistClass2Count.get(mode);
        if( dist2counts == null ) {
            dist2counts = new TreeMap<>();
            for(double cls : this.distClasses){
                dist2counts.put(cls, 0);
            }
            dist2counts.put(distClass, 1);
            this.mode2DistClass2Count.put(mode,dist2counts);
        } else {
            dist2counts.put(distClass, dist2counts.get(distClass)+1);
        }

        // store distances
        if (this.mode2PersonId2distances.containsKey(mode)) {
            Map<Id<Person>, List<Double>> personId2Dists = this.mode2PersonId2distances.get(mode);

            if(personId2Dists.containsKey(personId)) {
                List<Double> dists = personId2Dists.get(personId);
                dists.add(dist);
                personId2Dists.put(personId, dists);
            } else {
                List<Double> dists = new ArrayList<>();
                dists.add(dist);
                personId2Dists.put(personId,dists);
            }

        } else {
            Map<Id<Person>, List<Double>> personId2Dists = new TreeMap<>();
            List<Double> dists = new ArrayList<>();
            dists.add(dist);
            personId2Dists.put(personId, dists);
            this.mode2PersonId2distances.put(mode, personId2Dists);
        }
    }

    private double getDistanceBin(final double dist){
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

    public SortedMap<String, Map<Id<Person>, List<Double>>> getMode2PersonId2TravelDistances(){
        return this.mode2PersonId2distances;
    }
}