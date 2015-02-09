/* *********************************************************************** *
 * project: org.matsim.*
 * RandomizedTransitRouterNetworkTravelTimeAndDisutility2
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
package playground.pieter.distributed.randomizedcarrouter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;


/**
 * When plugged into the transit router, will switch to a different, randomly generated combination of
 * marginal utilities every time it is called for a new agent.
 * <p/>
 * Comments:<ul>
 * <li> In the literature, they seem to use log-normal distributions for the tastes.  (Makes sense
 * since it addresse the symmetry problem which I am addressing ad-hoc.) [M. K. Anderson, hEART'13]
 * kai, sep'13
 * <li> People seem to use "gamma distributions for the error term" ("double stochastic assignment").
 * Not sure what that does, if we need it, etc.  [M. K. Anderson, hEART'13] kai, sep'13
 * </ul>
 *
 * @author kai
 * @author dgrether
 */
public class RandomizedCarRouterTravelTimeAndDisutility implements TravelDisutility {

    private final TravelTime timeCalculator;
    private final double originalMarginalCostOfTime;
    private final double originalMarginalCostOfDistance;
    private Id cachedPersonId = null;
    private double localMarginalCostOfTime_s = Double.NaN;
    private double localMarginalCostOfDistance_m = Double.NaN;
    private Map<DataCollection, Boolean> dataCollectionConfig = new HashMap<DataCollection, Boolean>();
    private Map<DataCollection, StringBuffer> dataCollectionStrings = new HashMap<DataCollection, StringBuffer>();
    public RandomizedCarRouterTravelTimeAndDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
        this.prepareDataCollection();
        this.timeCalculator = timeCalculator;
        /* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
        this.originalMarginalCostOfTime = (-cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

//		this.marginalUtlOfDistance = cnScoringGroup.getMarginalUtlOfDistanceCar();
        this.originalMarginalCostOfDistance = -cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney();
        int wrnCnt = 0;
        if (wrnCnt < 1) {
            wrnCnt++;
            if (cnScoringGroup.getMonetaryDistanceCostRateCar() > 0.) {
                Logger.getLogger(this.getClass()).warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal" +
                        "behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.");
            }
        }
    }
    public RandomizedCarRouterTravelTimeAndDisutility(TravelTime timeCalculator, double marginalCostOfTime_s, double marginalCostOfDistance_m) {
        this.timeCalculator = timeCalculator;
        this.originalMarginalCostOfDistance = marginalCostOfDistance_m;
        this.originalMarginalCostOfTime = marginalCostOfTime_s;
    }

    @Override
    public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
        regenerateUtilityParametersIfPersonHasChanged(person);
        double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

        return this.localMarginalCostOfTime_s * travelTime + this.localMarginalCostOfDistance_m * link.getLength();
    }

    @Override
    public double getLinkMinimumTravelDisutility(final Link link) {

        return (link.getLength() / link.getFreespeed()) * this.localMarginalCostOfTime_s
                + this.localMarginalCostOfDistance_m * link.getLength();
    }

    public void setDataCollection(DataCollection item, Boolean bbb) {
        Logger.getLogger(this.getClass()).info(" setting data collection of " + item.toString() + " to " + bbb.toString());
        dataCollectionConfig.put(item, bbb);
    }

    private void prepareDataCollection() {
        for (DataCollection dataCollection : DataCollection.values()) {
            switch (dataCollection) {
                case randomizedParameters:
                    dataCollectionConfig.put(dataCollection, false);
                    dataCollectionStrings.put(dataCollection, new StringBuffer());
                    break;
                case additionalInformation:
                    dataCollectionConfig.put(dataCollection, false);
                    dataCollectionStrings.put(dataCollection, new StringBuffer());
                    break;
            }
        }
    }

    private void regenerateUtilityParametersIfPersonHasChanged(final Person person) {
        if (!person.getId().equals(this.cachedPersonId)) {
            // yyyyyy probably not thread safe (?!?!)

            // person has changed, so ...

            // ... memorize new person id:
            this.cachedPersonId = person.getId();

            // ... generate new random parameters:
            {
                double tmp = this.originalMarginalCostOfDistance;
                tmp *= 5. * MatsimRandom.getRandom().nextDouble();
                localMarginalCostOfDistance_m = tmp;
                // yy if this becomes too small, they may walk the whole way (is it really clear why this can happen?)
            }
            {
                double tmp = this.originalMarginalCostOfTime;
                tmp *= 5. * MatsimRandom.getRandom().nextDouble();
                localMarginalCostOfTime_s = tmp;
            }

            if (this.dataCollectionConfig.get(DataCollection.randomizedParameters)) {
//				StringBuffer strb = this.dataCollectionStrings.get(DataCollection.randomizedParameters) ;
//				strb.append
                System.out.println("personId: " + person.getId() +
                        "; marginalCostOfTime_h: " + this.localMarginalCostOfTime_s * 3600. +
                        "; marginalCostOfDistance_m: " + this.localMarginalCostOfDistance_m);
            }
        }
    }

    public enum DataCollection {randomizedParameters, additionalInformation}

}
