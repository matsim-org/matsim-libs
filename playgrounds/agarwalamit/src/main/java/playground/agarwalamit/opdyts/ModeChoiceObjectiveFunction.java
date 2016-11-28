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

import java.util.*;
import javax.inject.Inject;
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
import playground.agarwalamit.opdyts.patna.PatnaCMPDistanceDistribution;

/**
 *
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
public class ModeChoiceObjectiveFunction implements ObjectiveFunction {
    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger( ModeChoiceObjectiveFunction.class );

    private MainModeIdentifier mainModeIdentifier ;
    private final OpdytsScenarios opdytsScenarios;
    private final PatnaCMPDistanceDistribution distriInfo ;

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
    }

    private final Map<StatType,Databins<String>> simStatsContainer = new TreeMap<>() ;
    private final Map<StatType,DataMap<String>> sumsContainer  = new TreeMap<>() ;
    private final Map<StatType,Databins<String>> refStatsContainer = new TreeMap<>() ;


    public ModeChoiceObjectiveFunction(final OpdytsScenarios opdytsScenarios) {
        this.opdytsScenarios = opdytsScenarios;
        this.distriInfo = new PatnaCMPDistanceDistribution(opdytsScenarios);
        for ( StatType statType : StatType.values() ) {
            // define the bin boundaries:
            switch ( statType ) {
                case tripBeelineDistances: {
                    double[] dataBoundariesTmp = getDataBoundaries();
                    {
                        this.simStatsContainer.put( statType, new Databins<>( statType.name(), dataBoundariesTmp )) ;
                    }
                    {
                        final Databins<String> databins = new Databins<>( statType.name(), dataBoundariesTmp ) ;

                        fillDatabins(databins);
                        this.refStatsContainer.put( statType, databins) ;
                    }
                    break; }
                default:
                    throw new RuntimeException("not implemented") ;
            }
        }

        // for Patna, all legs have same trip mode.
        switch (opdytsScenarios){
            case EQUIL:
                mainModeIdentifier = new TransportPlanningMainModeIdentifier();
                break;
            case EQUIL_MIXEDTRAFFIC:
            case PATNA_1Pct:
            case PATNA_10Pct:
                mainModeIdentifier = new MainModeIdentifier() {
                    @Override
                    public String identifyMainMode(List<? extends PlanElement> tripElements) {
                        for (PlanElement pe : tripElements) {
                            if (pe instanceof Leg) return ((Leg) pe).getMode();
                        }
                        throw new RuntimeException("No instance of leg is found.");
                    }
                };
                break;
            default: throw new RuntimeException("not implemented");
        }
    }

    private void fillDatabins(Databins<String> databins) {
        switch (this.opdytsScenarios) {
            case EQUIL:
                double carVal = 1000. ;
                databins.addValue( TransportMode.car, 8, carVal);
                databins.addValue( TransportMode.pt, 8, 4000.-carVal);
                break;
            case EQUIL_MIXEDTRAFFIC:
                carVal = 1000.;
                databins.addValue( TransportMode.car, 8, carVal);
                databins.addValue( "bicycle", 8, 4000.- carVal);
                break;
            case PATNA_1Pct:
                double totalLegs = 13278.0 * 2.0; // each person make two trips; here trips are counted not persons.
                patnaModalDistDistribution(totalLegs,databins);
            break;
            case PATNA_10Pct:
                totalLegs = 13278.0 * 10 * 2.0; // each person make two trips; here trips are counted not persons.
                patnaModalDistDistribution(totalLegs,databins);
            default: throw new RuntimeException("not implemented");
        }
    }

    private void patnaModalDistDistribution(final double totalLegs, final Databins<String> databins){
        double legsSumAllModes = 0;
        SortedMap<String, double []> mode2distanceBasedLegs = distriInfo.getMode2DistanceBasedLegs();
        for (String mode : mode2distanceBasedLegs.keySet()) {
            double [] distBasedLegs = mode2distanceBasedLegs.get(mode);
            for (int idx = 0; idx < databins.getDataBoundaries().length; idx++) {
                databins.addValue(mode, idx, distBasedLegs[idx]);
            }
        }
    }

    private double [] getDataBoundaries() {
        switch (this.opdytsScenarios){
            case EQUIL:
            case EQUIL_MIXEDTRAFFIC:
                return new double[] {0., 100., 200., 500., 1000., 2000., 5000., 10000., 20000., 50000., 100000.};
            case PATNA_1Pct:
            case PATNA_10Pct:
                return distriInfo.getDistClasses();
            default: throw new RuntimeException("not implemented");
        }
    }

    @Override
    public double value(SimulatorState state) {

        resetContainers();

        StatType statType = StatType.tripBeelineDistances;

        for (Plan plan : service.getExperiencedPlans().values()) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, tripRouter.getStageActivityTypes());
            for (TripStructureUtils.Trip trip : trips) {
                List<String> tripTypes = new ArrayList<>();
                String mode = mainModeIdentifier.identifyMainMode(trip.getLegsOnly());
                tripTypes.add(mode);
                double item = calcBeelineDistance(trip.getOriginActivity(), trip.getDestinationActivity());
                addItemToAllRegisteredTypes(tripTypes, statType, item);
            }
        }

         /*
          *  From Gunnar,
          *  The (..)^2 objective function becomes very steep when one is very far away from its minimum.
          *  Meaning that once one is far off, it becomes difficult to get back because
          *  then the effects even of very small decision variable variations get strongly amplified in the objective function.
          */
//        switch (this.opdytsScenarios) {
//            case EQUIL:
//            case EQUIL_MIXEDTRAFFIC:
//            case PATNA_1Pct:
//            case PATNA_10Pct:
//                return getSumOfSquareObjectiveFunctionValue();
//            default:
//                throw new RuntimeException("not implemented yet.");
//        }

        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = new ObjectiveFunctionEvaluator();
        double objectiveFnValue = 0.;

        for ( Map.Entry<StatType, Databins<String>> entry : simStatsContainer.entrySet() ) {
            StatType theStatType = entry.getKey();  // currently only one type ;
            log.warn("statType=" + theStatType);
            Databins<String> databins = entry.getValue();
            objectiveFnValue = objectiveFunctionEvaluator.getObjectiveFunctionValue(entry.getValue(), this.refStatsContainer.get(theStatType));
        }
        return objectiveFnValue;
    }

//    private double getSumOfAbsoluteErrorsObjectiveFunctionValue() {
//        double objective = 0. ;
//        for ( Map.Entry<StatType, Databins<String>> entry : simStatsContainer.entrySet() ) {
//            StatType theStatType = entry.getKey() ;  // currently only one type ;
//            log.warn( "statType=" + theStatType );
//            if(! theStatType.equals(StatType.tripBeelineDistances)) throw new RuntimeException("not implemented yet.");
//
//            Databins<String> databins = entry.getValue() ;
//            for ( Map.Entry<String, double[]> theEntry : databins.entrySet() ) {
//                String mode = theEntry.getKey() ;
//                log.warn("mode=" + mode);
//
//                double[] simValue = theEntry.getValue() ;
//                double[] reaVal = this.refStatsContainer.get( theStatType).getValues(mode) ;
//                for ( int ii=0 ; ii<simValue.length ; ii++ ) {
//                    double diff = Math.abs( simValue[ii] - reaVal[ii] ) ;
//                    if ( reaVal[ii]>0.1 || simValue[ii]>0.1 ) {
//                        log.warn( "distanceBnd=" + databins.getDataBoundaries()[ii] + "; objVal=" + reaVal[ii] + "; simVal=" + simValue[ii] ) ;
//                    }
//                    objective +=  diff ;
//                }
//            }
//        }
//        log.warn( "objective=" + objective );
//        return objective ;
//    }
//
//    private double getSumOfSquareObjectiveFunctionValue(){
//        log.error("This should be used with great caution, with Patna, it was not a good experience. See email from GF on 24.11.2016");
//        double objective = 0. ;
//        double sum = 0.;
//        for ( Map.Entry<StatType, Databins<String>> entry : simStatsContainer.entrySet() ) {
//            StatType theStatType = entry.getKey() ;  // currently only one type ;
//            log.warn( "statType=" + theStatType );
//            Databins<String> databins = entry.getValue() ;
//            for ( Map.Entry<String, double[]> theEntry : databins.entrySet() ) {
//                String mode = theEntry.getKey() ;
//                log.warn("mode=" + mode);
//                double[] simValue = theEntry.getValue() ;
//                double[] reaVal = this.refStatsContainer.get( theStatType).getValues(mode) ;
//                for ( int ii=0 ; ii<simValue.length ; ii++ ) {
//                    double diff = simValue[ii] - reaVal[ii] ;
//                    if ( reaVal[ii]>0.1 || simValue[ii]>0.1 ) {
//                        log.warn( "distanceBnd=" + databins.getDataBoundaries()[ii] + "; objVal=" + reaVal[ii] + "; simVal=" + simValue[ii] ) ;
//                    }
//                    objective += diff * diff ;
//                    sum += reaVal[ii] ;
//                }
//            }
//        }
//        objective /= (sum*sum) ;
//        log.warn( "objective=" + objective );
//        return objective ;
//    }

    private void resetContainers() {
        for ( StatType statType : StatType.values() ) {
            this.simStatsContainer.get(statType).clear() ;
            if ( this.sumsContainer.get(statType)==null ) {
                this.sumsContainer.put( statType, new DataMap<>() ) ;
            }
            this.sumsContainer.get(statType).clear() ;
        }
    }

    private void addItemToAllRegisteredTypes(List<String> filters, StatType statType, double item) {
        // ... go through all filter to which the item belongs ...
        for ( String filter : filters ) {

            // ...  add the "item" to the correct bin in the container:
            int idx = this.simStatsContainer.get(statType).getIndex(item) ;
            this.simStatsContainer.get(statType).inc( filter, idx ) ;

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