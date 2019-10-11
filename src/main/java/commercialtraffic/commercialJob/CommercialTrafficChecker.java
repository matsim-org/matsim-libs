/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package commercialtraffic.commercialJob;


import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;

import java.util.Map;

class CommercialTrafficChecker {
    private static final Logger log = Logger.getLogger(CommercialTrafficChecker.class);

    static void run(Population population, Carriers carriers){
        boolean fail = checkPopulationAttributesConsistency(population);
        if(checkCarrierConsistency(carriers)) fail = true;
        if(fail) throw new RuntimeException("there is a problem with consistency for commercial traffic either in the plans or in the carriers. Please check the log for details.");
    }

    /**
     * @param population to check
     * @return true if errors exist, false if all attributes are set
     */
    private static boolean checkPopulationAttributesConsistency(Population population) {
        final MutableBoolean fail = new MutableBoolean(false);
        for (Person p : population.getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                for (Activity activity : CommercialJobUtils.getCustomerActivitiesExpectingJobs(plan)) {
                        if (checkActivityConsistency(activity, p.getId())) fail.setTrue();
                    }
            }
        }
        return fail.booleanValue();
    }

    private static boolean checkActivityConsistency(Activity activity, Id<Person> pid) {
        boolean fail = false;
        Map<String, Object> attributes = CommercialJobUtils.getCommercialJobAttributes(activity);
        for (String attribute : attributes.keySet()) {
            if(attribute.endsWith("0")){
                log.error("index 0 is not supported for commercial job attributes. please start with index 1. See activity " + activity + " of person " + pid);
                fail = true;
            }
            String[] jobProperties = String.valueOf(attributes.get(attribute)).split(CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_DELIMITER);
            if(jobProperties.length != 6){
                log.error("Activity " + activity + " of person " + pid + " defines commercialJob attribute " + attribute + " with a wrong number of properties. Length should be 6");
                fail = true;
            }
            Double timeWindowStart = Double.valueOf(jobProperties[CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_START_IDX]);
            Double timeWindowEnd = Double.valueOf(jobProperties[CommercialJobUtils.COMMERCIALJOB_ATTRIBUTE_END_IDX]);
            if (timeWindowEnd < timeWindowStart) {
                log.error("Person " + pid + " has an error in properties of job attribute " + attribute + " in activity " + activity.getType() + ".TimeWindow: start=" + timeWindowStart + " end=" + timeWindowEnd);
                fail = true;
            }
        }
        return fail;
    }

    /**
     * @param carriers to check
     * @return true if errors exist, false if carriers are set properly
     */
    private static boolean checkCarrierConsistency(Carriers carriers) {
        boolean fail = false;
        for (Carrier carrier : carriers.getCarriers().values()) {
            if (carrier.getId().toString().split(CommercialJobUtils.CARRIERSPLIT).length != 2) {
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
