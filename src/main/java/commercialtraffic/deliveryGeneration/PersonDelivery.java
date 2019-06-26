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

package commercialtraffic.deliveryGeneration;/*
 * created by jbischoff, 11.04.2019
 */


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PersonDelivery {

    public static final String DELIEVERY_SIZE = "deliveryAmount";
    public static final String DELIEVERY_TYPE = "deliveryType";
    public static final String DELIEVERY_DURATION = "deliveryDuration";
    public static final String DELIEVERY_TIME_START = "deliveryTimeStart";
    public static final String SERVICE_OPERATOR = "operator";
    public static final String DELIEVERY_TIME_END = "deliveryTimeEnd";

    public static final String CARRIERSPLIT = "_";


    public static Id<Carrier> getCarrierId(Activity activity) {
        return Id.create(activity.getAttributes().getAttribute(PersonDelivery.DELIEVERY_TYPE).toString() + CARRIERSPLIT + activity.getAttributes().getAttribute(PersonDelivery.SERVICE_OPERATOR).toString(), Carrier.class);
    }

    public static String getServiceOperator(Activity activity) {
        return activity.getAttributes().getAttribute(SERVICE_OPERATOR).toString();
    }

    public static void setServiceOperator(Activity activity, String operator) {
        activity.getAttributes().putAttribute(SERVICE_OPERATOR, operator);
    }

    public static void setServiceOperatorAndDeliveryType(Activity activity, Id<Carrier> carrierId) {
        String[] carrierString = carrierId.toString().split(CARRIERSPLIT);
        setDeliveryType(activity, carrierString[0]);
        setServiceOperator(activity, carrierString[1]);


    }


    public static String getDeliveryType(Activity activity) {
        return (String) activity.getAttributes().getAttribute(DELIEVERY_TYPE);
    }

    public static void setDeliveryType(Activity activity, String operator) {
        activity.getAttributes().putAttribute(DELIEVERY_TYPE, operator);
    }

    public static Set<Id<Carrier>> getOperatorsForDeliveryType(Carriers carriers, String deliveryType) {
        return carriers.getCarriers().values().
                stream().
                filter(carrier -> carrier.getId().toString().startsWith(deliveryType)).map(Carrier::getId).
                collect(Collectors.toSet());

    }

    public static Id<Carrier> getCarrierIdFromDriver(Id<Person> personId) {
        return Id.create(personId.toString().split(CARRIERSPLIT)[1] + CARRIERSPLIT + personId.toString().split(CARRIERSPLIT)[2], Carrier.class);
    }

    public static boolean planExpectsDeliveries(Plan plan) {
        return plan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .anyMatch(planElement -> planElement.getAttributes().getAsMap().containsKey(DELIEVERY_TYPE));
    }

    public static String getCarrierMarket(Id<Carrier> carrierId) {
        return carrierId.toString().split(CARRIERSPLIT)[0];
    }

    public static String getCarrierOperator(Id<Carrier> carrierId) {
        return carrierId.toString().split(CARRIERSPLIT)[1];
    }

    public static Map<String, Set<Id<Carrier>>> splitCarriersByMarket(Carriers carriers) {
        Map<String, Set<Id<Carrier>>> carriersSplitByMarket = new HashMap<>();
        for (Id<Carrier> carrierId : carriers.getCarriers().keySet()) {
            String market = getCarrierMarket(carrierId);
            Set<Id<Carrier>> carriersForMarket = carriersSplitByMarket.getOrDefault(market, new HashSet<>());
            carriersForMarket.add(carrierId);
            carriersSplitByMarket.put(market, carriersForMarket);
        }
        return carriersSplitByMarket;
    }

}
