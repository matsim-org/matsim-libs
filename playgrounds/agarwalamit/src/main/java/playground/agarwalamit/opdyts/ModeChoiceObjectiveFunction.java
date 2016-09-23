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

package playground.agarwalamit.opdyts;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import org.apache.log4j.Logger;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.analysis.kai.DataMap;
import org.matsim.contrib.analysis.kai.Databins;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.geometry.CoordUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
class ModeChoiceObjectiveFunction implements ObjectiveFunction {
    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( ModeChoiceObjectiveFunction.class );

    private MainModeIdentifier mainModeIdentifier ;

    @Inject ExperiencedPlansService service ;
    @Inject TripRouter tripRouter ;
    @Inject Network network ;
    // Documentation: "Guice injects ... fields of all values that are bound using toInstance(). These are injected at injector-creation time."
    // https://github.com/google/guice/wiki/InjectionPoints
    // I read that as "the fields are injected (every time again) when the instance is injected".
    // This is the behavior that we want here.  kai, sep'16

    // statistics types:
    enum StatType {
        tripBeelineDistances
    } ;

    private final Map<StatType,Databins<String>> statsContainer = new TreeMap<>() ;
    private final Map<StatType,DataMap<String>> sumsContainer  = new TreeMap<>() ;
    private final Map<StatType,Databins<String>> meaContainer = new TreeMap<>() ;


    ModeChoiceObjectiveFunction(final boolean isUsingPatna) {
        for ( StatType statType : StatType.values() ) {
            // define the bin boundaries:
            switch ( statType ) {
                case tripBeelineDistances: {
                    double[] dataBoundariesTmp = {0., 100., 200., 500., 1000., 2000., 5000., 10000., 20000., 50000., 100000.} ;
                    {
                        final Databins<String> databins = new Databins<>( statType.name(), dataBoundariesTmp );
                        this.statsContainer.put( statType, databins) ;
                    }
                    {
                        final Databins<String> databins = new Databins<>( statType.name(), dataBoundariesTmp ) ;
//					final double carVal = 3753.; // relaxed
                        final double carVal = 1000. ;
                        databins.addValue( TransportMode.car, 8, carVal);

                        if(isUsingPatna) databins.addValue( "bicycle", 8, 4000.-carVal);
                        else databins.addValue( TransportMode.pt, 8, 4000.-carVal);

                        this.meaContainer.put( statType, databins) ;
                    }
                    break; }
                default:
                    throw new RuntimeException("not implemented") ;
            }
        }

        // for Patna, all legs have same trip mode.
        if(isUsingPatna) {
            mainModeIdentifier = new MainModeIdentifier() {
                @Override
                public String identifyMainMode(List<? extends PlanElement> tripElements) {
                    for (PlanElement pe : tripElements) {
                        if (pe instanceof Leg) return ((Leg) pe).getMode();
                    }
                    throw new RuntimeException("No instance of leg is found.");
                }
            };
        } else {
            mainModeIdentifier = new TransportPlanningMainModeIdentifier();
        }
    }

    @Override public double value(SimulatorState state) {

        resetContainers();

        StatType statType = StatType.tripBeelineDistances ;

        for ( Plan plan : service.getExperiencedPlans().values() ) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, tripRouter.getStageActivityTypes() ) ;
            for ( TripStructureUtils.Trip trip : trips ) {
                List<String> tripTypes = new ArrayList<>() ;
                String mode = mainModeIdentifier.identifyMainMode(trip.getLegsOnly());
                tripTypes.add(mode) ;
                double item = calcBeelineDistance( trip.getOriginActivity(), trip.getDestinationActivity() ) ;
                addItemToAllRegisteredTypes(tripTypes, statType, item);
            }
        }

        double objective = 0. ;
        double sum = 0.;
        for ( Map.Entry<StatType, Databins<String>> entry : statsContainer.entrySet() ) {
            StatType theStatType = entry.getKey() ;  // currently only one type ;
            log.warn( "statType=" + statType );
            Databins<String> databins = entry.getValue() ;
            for ( Map.Entry<String, double[]> theEntry : databins.entrySet() ) {
                String mode = theEntry.getKey() ;
                log.warn("mode=" + mode);
                double[] value = theEntry.getValue() ;
                double[] reaVal = this.meaContainer.get( theStatType).getValues(mode) ;
                for ( int ii=0 ; ii<value.length ; ii++ ) {
                    double diff = value[ii] - reaVal[ii] ;
                    if ( reaVal[ii]>0.1 || value[ii]>0.1 ) {
                        log.warn( "distanceBnd=" + databins.getDataBoundaries()[ii] + "; objVal=" + reaVal[ii] + "; simVal=" + value[ii] ) ;
                    }
                    objective += diff * diff ;
                    sum += reaVal[ii] ;
                }
            }
        }
        objective /= (sum*sum) ;
        log.warn( "objective=" + objective );
        return objective ;

    }

    private void resetContainers() {
        for ( StatType statType : StatType.values() ) {
            this.statsContainer.get(statType).clear() ;
            if ( this.sumsContainer.get(statType)==null ) {
                this.sumsContainer.put( statType, new DataMap<String>() ) ;
            }
            this.sumsContainer.get(statType).clear() ;
        }
    }

    private void addItemToAllRegisteredTypes(List<String> filters, StatType statType, double item) {
        // ... go through all filter to which the item belongs ...
        for ( String filter : filters ) {

            // ...  add the "item" to the correct bin in the container:
            int idx = this.statsContainer.get(statType).getIndex(item) ;
            this.statsContainer.get(statType).inc( filter, idx ) ;

            // also add it to the sums container:
            this.sumsContainer.get(statType).addValue( filter, item ) ;

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
				log.warn("either fromAct or to Act has no Coord; using link coordinates as substitutes.") ;
				log.warn(Gbl.ONLYONCE ) ;
            }
            Link fromLink = network.getLinks().get( fromAct.getLinkId() ) ;
            Link   toLink = network.getLinks().get(   toAct.getLinkId() ) ;
            item = CoordUtils.calcEuclideanDistance( fromLink.getCoord(), toLink.getCoord() ) ;
        }
        return item;
    }



}
