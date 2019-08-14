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
 * modified by tschlenther august 2019
 */

import commercialtraffic.jobGeneration.CommercialJobUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Carriers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommercialTrafficChecker {
    private static final Logger log = Logger.getLogger(CommercialTrafficChecker.class);
    private final Integer MAXWARNCOUNT = 10;
    private int warnCount = 1;

    private Set<Id<CarrierService>> allServiceIdsReferencedInPopulation = new HashSet<>();

    public boolean checkActivityServiceLocationConsistency(Activity activity, Id<Person> pid, Map<Id<CarrierService>, CarrierService> services) {
        if(! CommercialJobUtils.activityExpectsServices(activity)) throw new RuntimeException();
        boolean fail = false;
        boolean actHasLinkId = activity.getLinkId() != null;
        if (!actHasLinkId && warnCount <= MAXWARNCOUNT) {
            log.warn("activity " + activity + " of agent " + pid + " references at least one service and has no link id set.\n" +
                    "please check if the service's link ids correspond to the activity location.\n" +
                    "Otherwise, results will be wrong and should not be interpreted!\n" +
                    "This message is only given " + MAXWARNCOUNT + " times");
            warnCount++;
        }

        Set<Id<CarrierService>> jobIds = CommercialJobUtils.getServiceIdsFromActivity(activity);
        for (Id<CarrierService> serviceId : jobIds) {
            if(!this.allServiceIdsReferencedInPopulation.add(serviceId)){
                log.error("service id=" + serviceId + " is contained at least twice in population file");
                fail = true;
            }
            if (!services.containsKey(serviceId)){
                log.error("Activity " + activity + " of person " + pid + " references a service which does not exist in input carriers file. serviceId=" + serviceId);
                fail = true;
            }
            if(actHasLinkId){
                if (!services.get(serviceId).getLocationLinkId().equals(activity.getLinkId())){
                    log.error("linkId's of service " + serviceId + " and activity " + activity + " of person " + pid + " do not match!");
                    fail = true;
                }
            }
        }
        return fail;
    }

    /**
     * @param carriers to check
     * @return true if errors exist, false if carriers are set properly
     */
    boolean checkCarrierConsistency(Carriers carriers, CommercialTrafficConfigGroup commercialTrafficConfigGroup) {
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
            if(!commercialTrafficConfigGroup.getRunTourPlanning()){
                if(carrier.getPlans().isEmpty()) {
                    log.error("carrier " + carrier.getId() + " does not have a plan but tour planning is switched off in CommercialTrafficConfigGroup");
                    fail = true;
                } else if(carrier.getPlans().get(0).getScheduledTours().isEmpty()){
                    log.error("the plan of carrier " + carrier.getId() + " does not have tour but tour planning is switched off in CommercialTrafficConfigGroup");
                    fail = true;
                }
            }
        }
        return fail;
    }

}
