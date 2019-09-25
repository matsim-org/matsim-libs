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

import commercialtraffic.integration.CarrierJSpritIterations;
import commercialtraffic.integration.CommercialTrafficConfigGroup;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class CommercialJobManager implements BeforeMobsimListener, AfterMobsimListener {

    private final FreightAgentInserter agentInserter;
    private final Scenario scenario;
    private final CommercialTrafficConfigGroup ctConfigGroup;
    private Carriers carriers;
    private Set<CarrierService> serviceRegistry = new HashSet<>();
    private Map<Id<CarrierService>,Id<Carrier>> service2Operator = new HashMap<>();
    private Map<Id<CarrierService>,Id<Person>> service2Customer = new HashMap<>();
    private TravelTime carTravelTime;

    private CarrierJSpritIterations carrierJSpritIterations;

    //needs to be public to be accessible in tests
    @Inject
    public CommercialJobManager(Carriers carriers, CarrierJSpritIterations carrierJSpritIterations, Scenario scenario, FreightAgentInserter agentInserter, Map<String, TravelTime> travelTimes){
        this.carrierJSpritIterations = carrierJSpritIterations;
        this.agentInserter = agentInserter;
        this.scenario = scenario;
        this.carTravelTime = travelTimes.get(TransportMode.car);
        this.ctConfigGroup = CommercialTrafficConfigGroup.get(scenario.getConfig());
        carriers.getCarriers().values().forEach(carrier -> {

            carrier.getServices().forEach(carrierService -> {
                this.serviceRegistry.add(carrierService);
                if(this.service2Operator.containsKey(carrierService.getId())) throw new IllegalArgumentException("duplicate service id: " + carrierService.getId());
                service2Operator.put(carrierService.getId(),carrier.getId());
            });
            if(ctConfigGroup.getRunTourPlanning()) carrier.getServices().clear(); //initialize
        });
        this.carriers = carriers;
        if(mapServicesToCustomer(scenario.getPopulation()))
            throw new RuntimeException("there is a problem with consistency of location in services and activities." +
                "please check the log for details.");
    }


    private boolean mapServicesToCustomer(Population population){
        final MutableBoolean fail = new MutableBoolean(false);
        for (Person p : population.getPersons().values()) {
            p.getPlans()
                .forEach(plan -> {
                    plan.getPlanElements().stream()
                        .filter(Activity.class::isInstance)
                        .filter(planElement -> CommercialJobUtils.activityExpectsServices((Activity) planElement))
                            .forEach(planElement -> {
                            retrieveServicesIdsAndDoMapping(p.getId(),(Activity) planElement);
                        });
                });
        }
        return fail.getValue();
    }

    private void retrieveServicesIdsAndDoMapping(Id<Person> personId, Activity activity) {
        Set<Id<CarrierService>> jobIDs = CommercialJobUtils.getServiceIdsFromActivity(activity);
        for(Id<CarrierService> carrierServiceId : jobIDs){
            if(this.service2Customer.containsKey(carrierServiceId)) throw new IllegalArgumentException("service id " + carrierServiceId + " is referenced twice in input population");
            this.service2Customer.put(carrierServiceId,personId);
        }
    }


    public void setOperatorForService(Id<CarrierService> serviceId, Id<Carrier> carrierId){
        if(! this.service2Operator.containsKey(serviceId)) throw new IllegalArgumentException("there is no registration of service " + serviceId);
        if(! this.carriers.getCarriers().containsKey(carrierId)) throw new IllegalArgumentException("no carrier with id " + carrierId + " was initially defined in the carriers input file");
        this.service2Operator.put(serviceId,carrierId);
    }

    private String getServiceTypeOfCarrier(Id<Carrier> carrierId){
        String[] idString =  carrierId.toString().split(CommercialJobUtils.CARRIERSPLIT);
        if(idString.length != 2) throw new IllegalArgumentException("illegal carrier id. please make sure that carrier id's are set to the pattern serviceType" + CommercialJobUtils.CARRIERSPLIT + "operator!"
                + "\n this is not the case for " + carrierId);
        return idString[0];
    }

    public Id<Person> getCustomer(@NotNull Id<CarrierService> serviceId){
        return this.service2Customer.get(serviceId);
    }

    public Set<Id<Person>> getAllCustomers(){
        Set<Id<Person>> allCustomers = new HashSet<>(this.service2Customer.values());
        return Collections.unmodifiableSet( allCustomers );
    }

    public String getServiceType(Id<CarrierService> serviceId){
        return getServiceTypeOfCarrier(this.service2Operator.get(serviceId));
    }

    public Set<Id<Carrier>> getOperatorsForDeliveryType(String serviceType) {
        return this.carriers.getCarriers().keySet().
                stream().
                filter(carrierId -> getServiceTypeOfCarrier(carrierId).equals(serviceType) )
                .collect(Collectors.toSet());
    }

    public Map<Id<CarrierService>,CarrierService> getCarrierServicesMap(){
        return Collections.unmodifiableMap(serviceRegistry.stream().collect(Collectors.toMap(CarrierService::getId, s -> s, (a, b) -> b)) );
    }

    public Id<Carrier> getCurrentCarrierOfService(Id<CarrierService> service) {
        return this.service2Operator.get(service);
    }

    /**
     * Notifies all observers of the Controler that the mobility simulation will start next.
     *
     * @param event
     */
    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        if(ctConfigGroup.getRunTourPlanning()){
            serviceRegistry.forEach(service -> carriers.getCarriers().get(service2Operator.get(service.getId())).getServices().add(service));
            TourPlanning.runTourPlanningForCarriers(carriers,scenario, carrierJSpritIterations, ctConfigGroup.getJspritTimeSliceWidth(), carTravelTime);
            agentInserter.createFreightAgents(carriers,ctConfigGroup.getFirstLegTraveltimeBufferFactor());
        }
    }

    /**
     * Notifies all observers of the Controler that the mobility simulation just finished.
     *
     * @param event
     */
    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        if(ctConfigGroup.getRunTourPlanning()){
            writeCarriersFileForIteration(event);
            this.agentInserter.removeFreightAgents(carriers);
            carriers.getCarriers().values().forEach(carrier -> {
                carrier.getServices().clear();
                carrier.getShipments().clear();
                carrier.clearPlans();
            });
        }
    }

    private void writeCarriersFileForIteration(AfterMobsimEvent event) {
        String dir = event.getServices().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/";
//        log.info("writing carrier file of iteration " + event.getIteration() + " to " + dir);
        CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers.getCarriers().values());
        planWriter.write(dir + "carriers_it" + event.getIteration() + ".xml");
    }

}
