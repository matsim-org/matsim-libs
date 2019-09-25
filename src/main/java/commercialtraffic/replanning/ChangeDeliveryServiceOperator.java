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

package commercialtraffic.replanning;/*
 * created by jbischoff, 22.05.2019
 */

import commercialtraffic.commercialJob.CommercialJobUtils;
import commercialtraffic.commercialJob.CommercialJobUtilsV2;
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
 * Changes the carrier for a single commercial delivery of the day.
 */
public class ChangeDeliveryServiceOperator extends AbstractMultithreadedModule {

    public static final String SELECTOR_NAME = "changeDeliveryServiceOperator";

    private final Carriers carriers;
    private final Random random;

    public ChangeDeliveryServiceOperator(GlobalConfigGroup globalConfigGroup, Carriers carriers) {
        super(globalConfigGroup);
        this.carriers = carriers;
        random = MatsimRandom.getLocalInstance();
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        return plan -> {
            List<Activity> activitiesWithServices = new ArrayList<>();
            plan.getPlanElements().stream().filter(Activity.class::isInstance).filter(a -> a.getAttributes().getAsMap().containsKey(CommercialJobUtilsV2.JOB_TYPE)).forEach(planElement -> activitiesWithServices.add((Activity) planElement));
            if (activitiesWithServices.isEmpty()) {
                return;
            }
            int idx = random.nextInt(activitiesWithServices.size());

            Activity selectedActivity = activitiesWithServices.get(idx);
            String deliveryType = CommercialJobUtilsV2.getDeliveryType(selectedActivity);
            Set<Id<Carrier>> operators4Service = CommercialJobUtilsV2.getOperatorsForDeliveryType(carriers, deliveryType);
            Id<Carrier> currentCarrier = CommercialJobUtilsV2.getCarrierId(selectedActivity);

            if (operators4Service.remove(currentCarrier)) {
                if (!operators4Service.isEmpty()) {
                    Id<Carrier> newCarrier = operators4Service.stream().skip(random.nextInt(operators4Service.size())).findFirst().orElse(currentCarrier);
                    CommercialJobUtilsV2.setServiceOperatorAndDeliveryType(selectedActivity, newCarrier);
                }
            } else
                throw new RuntimeException(currentCarrier.toString() + " is not part of the service carriers for deliverytype " + deliveryType);
        };
    }
}
