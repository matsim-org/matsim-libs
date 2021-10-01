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

package org.matsim.contrib.commercialTrafficApplications.jointDemand.commercialJob;/*
 * created by jbischoff, 22.05.2019
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Changes the carrier for a single, random commercial job in the given plan. <br>
 * Can only choose carriers that operate within the same market. The market is (assumed to be) an attribute of the carrier.
 */
public final class ChangeCommercialJobOperator extends AbstractMultithreadedModule {

    public static final String SELECTOR_NAME = "changeCommercialJobOperator";

    private final Carriers carriers;

	ChangeCommercialJobOperator(GlobalConfigGroup globalConfigGroup, Carriers carriers) {
        super(globalConfigGroup);
        this.carriers = carriers;
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        Random random = MatsimRandom.getLocalInstance();
        return plan -> {
            List<Activity> activitiesWithJobs = new ArrayList<>(JointDemandUtils.getCustomerActivitiesExpectingJobs(plan));
            if (activitiesWithJobs.isEmpty()) {
                return;
            }
            int randomActIdx = random.nextInt(activitiesWithJobs.size());

            Activity selectedActivity = activitiesWithJobs.get(randomActIdx);
            int randomJobIdx = random.nextInt(JointDemandUtils.getNumberOfJobsForActivity(selectedActivity)) + 1; //the smallest index is 1.

            Id<Carrier> currentCarrier = JointDemandUtils.getCurrentlySelectedCarrierForJob(selectedActivity, randomJobIdx);
            String market = JointDemandUtils.getCarrierMarket(carriers.getCarriers().get(currentCarrier));
            Set<Id<Carrier>> operators4Market = JointDemandUtils.getExistingOperatorsForMarket(carriers, market);

            if (operators4Market.remove(currentCarrier)) {
                if (!operators4Market.isEmpty()) {
                    Id<Carrier> newCarrier = operators4Market.stream().skip(random.nextInt(operators4Market.size())).findFirst().orElse(currentCarrier);
                    JointDemandUtils.setJobCarrier(selectedActivity, randomJobIdx, newCarrier);
                }
            } else
                throw new RuntimeException(currentCarrier.toString() + " is not part of the commercial traffic carriers for market " + market);
        };
    }
}
