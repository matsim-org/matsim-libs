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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;/*
 * created by jbischoff, 17.06.2019
 */

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarrierConstants;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.*;
import java.util.stream.Collectors;

class ScoreCommercialJobs implements ActivityStartEventHandler, ActivityEndEventHandler {

    private final Population population;

    private final CommercialJobScoreCalculator scoreCalculator;
    private final EventsManager eventsManager;

    private final Set<Id<Person>> activeDeliveryAgents = new HashSet<>();
    private final List<DeliveryLogEntry> logEntries = new ArrayList<>();
    private final Carriers carriers;
    private Map<Id<Person>, Queue<Id<CarrierService>>> freightAgent2Jobs = new HashMap<>();

    @Inject
    ScoreCommercialJobs(Population population, CommercialJobScoreCalculator scoreCalculator, EventsManager eventsManager, Carriers carriers) {
        this.population = population;
        this.scoreCalculator = scoreCalculator;
        this.eventsManager = eventsManager;
        this.eventsManager.addHandler(this);
        this.carriers = carriers;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (activeDeliveryAgents.contains(event.getPersonId())) {
            handleFreightActivityStart(event);
        }
    }


    void prepareTourArrivalsForDay() {
        freightAgent2Jobs.clear();

        Set<Plan> freightPlans = population.getPersons().values().stream()
                .filter(p -> p.getId().toString().startsWith(JointDemandUtils.FREIGHT_DRIVER_PREFIX))
                .map(HasPlansAndId::getSelectedPlan)
                .collect(Collectors.toSet());

        for (Plan freightPlan : freightPlans) {
            Id<Person> freightAgent = freightPlan.getPerson().getId();
            Queue<Id<CarrierService>> servicesServedByFreightAgent = new LinkedList<>();
            freightPlan.getPlanElements().stream()
                    .filter(pE -> pE instanceof Activity)
                    .filter(act -> ((Activity) act).getType().startsWith(CommercialJobGenerator.COMMERCIALJOB_ACTIVITYTYPE_PREFIX))
                    .forEach(act -> {
                        Id<CarrierService> serviceId = Id.create((String) act.getAttributes().getAttribute(CommercialJobGenerator.SERVICEID_ATTRIBUTE_NAME),
                                CarrierService.class);
                        servicesServedByFreightAgent.add(serviceId);
                    });
            this.freightAgent2Jobs.put(freightAgent, servicesServedByFreightAgent);
        }
    }


    private void handleFreightActivityStart(ActivityStartEvent event) {
        if (event.getActType().equals(CarrierConstants.END)) {
            activeDeliveryAgents.remove(event.getPersonId());
        } else if (event.getActType().startsWith(CommercialJobGenerator.COMMERCIALJOB_ACTIVITYTYPE_PREFIX)) {

            Id<Carrier> carrierId = JointDemandUtils.getCarrierIdFromDriver(event.getPersonId());
            Carrier carrier = carriers.getCarriers().get(carrierId);
            CarrierService job = carrier.getServices().get(freightAgent2Jobs.get(event.getPersonId()).poll());

            Id<Person> customerAboutToBeServed = Id.createPersonId((String) job.getAttributes().getAttribute(CommercialJobGenerator.CUSTOMER_ATTRIBUTE_NAME));

            double timeDifference = calcDifference(job, event.getTime());
            double score = scoreCalculator.calcScore(timeDifference);

            eventsManager.processEvent(new PersonScoreEvent(event.getTime(),customerAboutToBeServed,score,"jobStart_" + job.getId()));
            logEntries.add(new DeliveryLogEntry(customerAboutToBeServed, carrier.getId(), event.getTime(), score, event.getLinkId(), timeDifference, event.getPersonId()));
        }
    }

    private double calcDifference(CarrierService service, double time) {
        if (time < service.getServiceStaringTimeWindow().getStart()) return (service.getServiceStaringTimeWindow().getStart() - time);
        else if (time >= service.getServiceStaringTimeWindow().getStart() && time <= service.getServiceStaringTimeWindow().getEnd()) return 0;
        else return (time - service.getServiceStaringTimeWindow().getEnd());
    }

    @Override
    public void reset(int iteration) {
        activeDeliveryAgents.clear();
        logEntries.clear();
    }


    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals(CarrierConstants.START)) {
            activeDeliveryAgents.add(event.getPersonId());
        }
    }

    public List<DeliveryLogEntry> getLogEntries() {
        return logEntries;
    }

    public static class DeliveryLogEntry {
        private final Id<Person> personId;
        private final Id<Carrier> carrierId;
        private final double time;
        private final double score;
        private final Id<Link> linkId;
        private final double timeDifference;
        private final Id<Person> driverId;

        DeliveryLogEntry(Id<Person> personId, Id<Carrier> carrierId, double time, double score, Id<Link> linkId, double timeDifference, Id<Person> driverId) {
            this.personId = personId;
            this.carrierId = carrierId;
            this.time = time;
            this.score = score;
            this.linkId = linkId;
            this.timeDifference = timeDifference;
            this.driverId = driverId;
        }

        public Id<Person> getPersonId() {
            return personId;
        }

        public Id<Carrier> getCarrierId() {
            return carrierId;
        }

        public double getTime() {
            return time;
        }

        public double getScore() {
            return score;
        }

        public Id<Link> getLinkId() {
            return linkId;
        }

        public double getTimeDifference() {
            return timeDifference;
        }

        public Id<Person> getDriverId() {
            return driverId;
        }
    }
}
