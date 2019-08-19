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

import com.google.inject.Inject;
import commercialtraffic.jobGeneration.CommercialJobManager;
import commercialtraffic.jobGeneration.CommercialJobUtils;
import commercialtraffic.replanning.ChangeDeliveryServiceOperator;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommercialTrafficChecker implements MobsimInitializedListener {
    private static final Logger log = Logger.getLogger(CommercialTrafficChecker.class);

    @Inject
    Scenario scenario;

    @Inject
    CommercialJobManager commercialJobManager;

    private Set<Id<CarrierService>> allServiceIdsReferencedInPopulation = new HashSet<>();

    /**
     * @param carriers to check
     * @return true if errors exist, false if carriers are set properly
     */
    boolean checkCarrierConsistency(Carriers carriers, Config config) {

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
            if(!CommercialTrafficConfigGroup.get(config).getRunTourPlanning()){

                if(carrier.getPlans().isEmpty()) {
                    log.error("carrier " + carrier.getId() + " does not have a plan but tour planning is switched off in CommercialTrafficConfigGroup");
                    fail = true;
                } else if(carrier.getPlans().get(0).getScheduledTours().isEmpty()){
                    log.error("the plan of carrier " + carrier.getId() + " does not have tour but tour planning is switched off in CommercialTrafficConfigGroup");
                    fail = true;
                }
            } else if(config.strategy().getStrategySettings().stream()
                        .anyMatch(strategySettings -> strategySettings.getStrategyName().equals(ChangeDeliveryServiceOperator.SELECTOR_NAME))){
                if(!carrier.getCarrierCapabilities().getFleetSize().equals(CarrierCapabilities.FleetSize.INFINITE)){
                    log.error("at the moment, only infinite fleet size is allowed when using the " + ChangeDeliveryServiceOperator.SELECTOR_NAME
                    + " strategy. this way it is avoided, that agents that expect a service do not get served. fleetsize is not infinite for carrier " + carrier.getId());
                    fail = true;
                }
            }
        }
        return fail;
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent mobsimInitializedEvent) {
        this.allServiceIdsReferencedInPopulation.clear();
        final MutableBoolean fail = new MutableBoolean(false);
        Map<Id<CarrierService>, CarrierService> servicesMap = commercialJobManager.getCarrierServicesMap();
        commercialJobManager.getAllCustomers().forEach(customerId -> {
            Person person = this.scenario.getPopulation().getPersons().get(customerId);
            person.getSelectedPlan().getPlanElements().stream()
            .filter(Activity.class::isInstance)
            .filter(planElement -> CommercialJobUtils.activityExpectsServices((Activity) planElement))
            .forEach(planElement -> {
                if(this.checkActivityServiceLocationConsistency((Activity) planElement,person.getId(), servicesMap))
                    fail.setTrue();
            });
        });

        if(fail.getValue()){
            throw new RuntimeException("there is a problem with consistency of location in services and activities." +
                    "please check the log for details.");
        }
    }

    private boolean checkActivityServiceLocationConsistency(Activity activity, Id<Person> pid, Map<Id<CarrierService>, CarrierService> services) {
        if(! CommercialJobUtils.activityExpectsServices(activity)) throw new RuntimeException();
        boolean fail = false;
        boolean actHasLinkId = activity.getLinkId() != null;
        if (!actHasLinkId) {

            throw new RuntimeException("activity " + activity + " of agent " + pid + " references at least one service and has no link id set. (even after personPrepareForSim)\n" +
                    "At the time being, this is not accepted. Aborting...");
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
            if (!services.get(serviceId).getLocationLinkId().equals(activity.getLinkId())){
                log.error("linkId's of service " + serviceId + " and activity " + activity + " of person " + pid + " do not match!");
                fail = true;
            }
        }
        return fail;
    }

}
