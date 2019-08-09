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

package commercialtraffic.jobGeneration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Carriers;

import java.util.*;
import java.util.stream.Collectors;

public class CommercialJobManager {

    private Carriers carriers;
    private Set<CarrierService> serviceRegistry = new HashSet<>();
    private Map<Id<CarrierService>,Id<Carrier>> service2Operator = new HashMap<>();
//    private final Map<Id<CarrierService>,String> serviceType = new HashMap<>();

    CommercialJobManager(Carriers carriers){
        carriers.getCarriers().values().forEach(carrier -> {
            carrier.getServices().forEach(carrierService -> {
                this.serviceRegistry.add(carrierService);
                if(this.service2Operator.containsKey(carrierService.getId())) throw new IllegalArgumentException("duplicate service id: " + carrierService.getId());
                service2Operator.put(carrierService.getId(),carrier.getId());
            });
            carrier.getServices().clear(); //initialize
        });
        this.carriers = carriers;
    }

    public void setOperatorForService(Id<CarrierService> serviceId, Id<Carrier> carrierId){
        if(! this.service2Operator.containsKey(serviceId)) throw new IllegalArgumentException("there is no registration of service " + serviceId);
        if(! this.carriers.getCarriers().containsKey(carrierId)) throw new IllegalArgumentException("no carrier with id " + carrierId + " was initially defined in the carriers input file");
        this.service2Operator.put(serviceId,carrierId);
    }

    Carriers getCarriersWithServicesForIteration(){
        Carriers carriersForIteration = new Carriers();
        carriers.getCarriers().values().forEach(carriersForIteration::addCarrier);
        serviceRegistry.forEach(service -> carriersForIteration.getCarriers().get(service2Operator.get(service.getId())).getServices().add(service));
        return carriersForIteration;
        //TODO test whether this.carriers now also contain services => if so, write initialize method or create copies of carrier and their capabilities
    }


    private String getServiceTypeOfCarrier(Id<Carrier> carrierId){
        String[] idString =  carrierId.toString().split(CommercialJobUtils.CARRIERSPLIT);
        if(idString.length != 2) throw new IllegalArgumentException("illegal carrier id. please make sure that carrier id's are set to the pattern serviceType" + CommercialJobUtils.CARRIERSPLIT + "operator!"
                + "\n this is not the case for " + carrierId);
        return idString[0];
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

    Map<Id<CarrierService>,CarrierService> getCarrierServicesMap(){
        return Collections.unmodifiableMap(serviceRegistry.stream().collect(Collectors.toMap(CarrierService::getId, s -> s, (a, b) -> b)) );
    }


    public Id<Carrier> getCurrentCarrierOfService(Id<CarrierService> service) {
        return this.service2Operator.get(service);
    }
}
