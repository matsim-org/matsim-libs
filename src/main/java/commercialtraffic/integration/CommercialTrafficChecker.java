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

package commercialtraffic.integration;/*
 * created by jbischoff, 20.06.2019
 */

import commercialtraffic.deliveryGeneration.PersonDelivery;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CommercialTrafficChecker {
    private static final Logger log = Logger.getLogger(CommercialTrafficChecker.class);
    private static final List<String> attributesToCheck = Arrays.asList(PersonDelivery.JOB_OPERATOR, PersonDelivery.JOB_DURATION, PersonDelivery.JOB_TIME_END, PersonDelivery.JOB_EARLIEST_START, PersonDelivery.JOB_SIZE);

    /**
     * @param population to check
     * @return true if errors exist, false if all attributes are set
     */
    public static boolean hasMissingAttributes(Population population) {
        final MutableBoolean fail = new MutableBoolean(false);
        for (Person p : population.getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                plan.getPlanElements().stream().filter(Activity.class::isInstance).filter(planElement -> planElement.getAttributes().getAsMap().containsKey(PersonDelivery.JOB_TYPE)).forEach(planElement -> {
                    if (checkActivityConsistency((Activity) planElement, p.getId()) == true) {
                        fail.setTrue();
                    }
                });
            }
        }
        return fail.booleanValue();
    }

    private static boolean checkActivityConsistency(Activity activity, Id<Person> pid) {
        boolean fail = false;
        Map<String, Object> attributes = activity.getAttributes().getAsMap();
        for (String attribute : attributesToCheck) {
            if (!attributes.containsKey(attribute)) {
                log.error("Person" + pid + " lacks " + attribute + " in Activity " + activity.getType());
                fail = true;
            }
        }
        Double timeWindowStart = Double.valueOf(String.valueOf(activity.getAttributes().getAttribute(PersonDelivery.JOB_EARLIEST_START)));
        Double timeWindowEnd = Double.valueOf(String.valueOf(activity.getAttributes().getAttribute(PersonDelivery.JOB_TIME_END)));
        if (timeWindowEnd < timeWindowStart) {
            log.error("Person " + pid + " has an error in timewindows in Activity " + activity.getType() + ". start=" + timeWindowStart + " end=" +timeWindowEnd);
            fail = true;
        }
        return fail;
    }

    /**
     * @param carriers to check
     * @return true if errors exist, false if carriers are set properly
     */
    public static boolean checkCarrierConsistency(Carriers carriers) {
        boolean fail = false;
        for (Carrier carrier : carriers.getCarriers().values()) {
            if (carrier.getId().toString().split(PersonDelivery.CARRIERSPLIT).length != 2) {
                log.error("Carrier ID " + carrier.getId() + " does not conform to scheme good_carrier, e.g. pizza_one, pizza_two, ...");
                fail = true;
            }
            if (carrier.getCarrierCapabilities().getVehicleTypes().isEmpty()) {
                log.error("Carrier " + carrier.getId() + " needs to have at least one vehicle type defined");
                fail = true;
            }
            if (carrier.getCarrierCapabilities().getCarrierVehicles().isEmpty()) {
                log.error("Carrier " + carrier.getId() + " needs to have at least one vehicle defined.");
                fail = true;
            }
        }
        return fail;
    }

}
