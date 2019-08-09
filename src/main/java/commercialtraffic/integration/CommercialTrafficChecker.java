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

import commercialtraffic.jobGeneration.CommercialJobManager;
import commercialtraffic.jobGeneration.CommercialJobUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Carriers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CommercialTrafficChecker {
    private static final Logger log = Logger.getLogger(CommercialTrafficChecker.class);

    /**
     * checks whether the services referenced in the population have the corresponding link id's
     *
     * @param population containing references to services
     * @param services
     */
    public static void checkLinkIdsOfReferencedServicesInPopulation(Population population, Map<Id<CarrierService>, CarrierService> services) {
        final MutableBoolean fail = new MutableBoolean(false);
        for (Person p : population.getPersons().values()) {
            p.getPlans().parallelStream()
                    .forEach(plan -> {
                        plan.getPlanElements().stream()
                                .filter(Activity.class::isInstance)
                                .filter(planElement -> planElement.getAttributes()
                                .getAsMap().containsKey(CommercialJobUtils.JOB_ID)).forEach(planElement ->
                                    checkActivityConsistency((Activity) planElement, p.getId(), services));
                    });
        }
    }


    private static void checkActivityConsistency(Activity activity, Id<Person> pid, Map<Id<CarrierService>, CarrierService> services) {
        String[] jobIds = CommercialJobUtils.getServiceIdStringArrayFromActivity(activity);
        for (String jobId : jobIds) {
            Id<CarrierService> serviceId = Id.create(jobId, CarrierService.class);
            if (services.containsKey(serviceId))
                throw new IllegalStateException("Activity " + activity + " of person " + pid + " references a service which does not exist in input carriers file. serviceId=" + serviceId);
            if (!services.get(serviceId).getLocationLinkId().equals(activity.getLinkId()))
                throw new IllegalStateException("linkId's of service " + serviceId + " and activity " + activity + " of person " + pid + " do not match!");
        }
    }


    /**
     * @param carriers to check
     * @return true if errors exist, false if carriers are set properly
     */
    public static boolean checkCarrierConsistency(Carriers carriers) {
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
