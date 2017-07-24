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

package org.matsim.contrib.opdyts.modeChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import org.apache.log4j.Logger;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.kai.DataMap;
import org.matsim.contrib.analysis.kai.Databins;
import org.matsim.contrib.opdyts.MATSimState;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 *
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
public class ModeChoiceObjectiveFunction implements ObjectiveFunction {

    private static final Logger LOGGER = Logger.getLogger(ModeChoiceObjectiveFunction.class);

    private final MainModeIdentifier mainModeIdentifier ;

    @Inject private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
    @Inject private TripRouter tripRouter ;
    @Inject private Network network ;
    // Documentation: "Guice injects ... fields of all values that are bound using toInstance(). These are injected at injector-creation time."
    // https://github.com/google/guice/wiki/InjectionPoints
    // I read that as "the fields are injected (every time again) when the instance is injected".
    // This is the behavior that we want here.  kai, sep'16

    private Databins<String> simStatsContainer = null;
    private DataMap<String> sumsContainer  = null ;
    private Databins<String> refStatsContainer = null ;

     public ModeChoiceObjectiveFunction() {
            // define the bin boundaries:
         double[] dataBoundariesTmp = {0.} ;
         simStatsContainer =  new Databins<>( "simStats", dataBoundariesTmp ) ;

         this.refStatsContainer = new Databins<>( "measuredStats", dataBoundariesTmp ) ;
         this.refStatsContainer.addValue(TransportMode.car, 0, 1000.0);
         this.refStatsContainer.addValue(TransportMode.bike, 0, 3000.0);

         mainModeIdentifier = new TransportPlanningMainModeIdentifier();
    }

    @Override
    public double value(SimulatorState state) {
        resetContainers();

        MATSimState matSimState = (MATSimState) state;
        Set<Id<Person>> persons = matSimState.getPersonIdView();

        for (Id<Person> personId : persons) {
            Plan plan = matSimState.getSelectedPlan(personId);
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, tripRouter.getStageActivityTypes());
            for (TripStructureUtils.Trip trip : trips) {
                List<String> tripTypes = new ArrayList<>();
                String mode = mainModeIdentifier.identifyMainMode(trip.getLegsOnly());
                tripTypes.add(mode);
                double item = calcBeelineDistance(trip.getOriginActivity(), trip.getDestinationActivity());
                addItemToAllRegisteredTypes(tripTypes, item);
            }
        }

        double objectiveFnValue =0.;
        double realValueSum = 0;

        for ( Map.Entry<String, double[]> theEntry : simStatsContainer.entrySet() ) {
            String mode = theEntry.getKey() ;
            LOGGER.warn("mode=" + mode);
            double[] value = theEntry.getValue() ;
            double[] reaVal = this.refStatsContainer.getValues(mode) ;
            for ( int ii=0 ; ii<value.length ; ii++ ) {
                double diff = value[ii] - reaVal[ii] ;
                objectiveFnValue += diff * diff ;
                realValueSum += reaVal[ii] ;
            }
        }
        objectiveFnValue /= (realValueSum * realValueSum);
        return objectiveFnValue ;
    }

    private void resetContainers() {
        this.simStatsContainer.clear();
        if ( this.sumsContainer==null ) {
            this.sumsContainer = new DataMap<>() ;
        }
        this.sumsContainer.clear() ;
    }

    private void addItemToAllRegisteredTypes(List<String> filters, double item) {
        // ... go through all filter to which the item belongs ...
        for ( String filter : filters ) {
            // ...  add the "item" to the correct bin in the container:
            int idx = this.simStatsContainer.getIndex(item) ;
            this.simStatsContainer.inc( filter, idx ) ;

            // also add it to the sums container:
            this.sumsContainer.addValue( filter, item ) ;
        }
    }

    private static int noCoordCnt = 0 ;
    private double calcBeelineDistance(final Activity fromAct, final Activity toAct) {
        double item;
        if ( fromAct.getCoord()!=null && toAct.getCoord()!=null ) {
            item = CoordUtils.calcEuclideanDistance(fromAct.getCoord(), toAct.getCoord()) ;
        } else {
            if ( noCoordCnt < 1 ) {
                noCoordCnt ++ ;
                LOGGER.warn("either fromAct or to Act has no Coord; using link coordinates as substitutes.") ;
                LOGGER.warn(Gbl.ONLYONCE ) ;
            }
            Link fromLink = network.getLinks().get( fromAct.getLinkId() ) ;
            Link   toLink = network.getLinks().get(   toAct.getLinkId() ) ;
            item = CoordUtils.calcEuclideanDistance( fromLink.getCoord(), toLink.getCoord() ) ;
        }
        return item;
    }
}