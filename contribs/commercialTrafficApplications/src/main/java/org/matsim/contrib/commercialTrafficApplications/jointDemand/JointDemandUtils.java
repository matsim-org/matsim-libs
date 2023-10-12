/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.Carriers;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

//TODO add javadoc
public class JointDemandUtils {

    public static final String COMMERCIALJOB_ATTRIBUTE_NAME = "commercialJob";
    public static final String CARRIER_MARKET_ATTRIBUTE_NAME = "market";
    static final String FREIGHT_DRIVER_PREFIX = "freight";

//    the pattern is the following
//    attribute name="commercialJob[NUMBER]" class="java.util.Collection">["OPERATOR","CAPACITYDEMAND","EARLIESTSTART","LATESTSTART","DURATION"]</attribute>
    public static final int COMMERCIALJOB_ATTRIBUTE_CARRIER_IDX = 0;
    public static final int COMMERCIALJOB_ATTRIBUTE_AMOUNT_IDX = 1;
    public static final int COMMERCIALJOB_ATTRIBUTE_START_IDX = 2;
    public static final int COMMERCIALJOB_ATTRIBUTE_END_IDX = 3;
    public static final int COMMERCIALJOB_ATTRIBUTE_DURATION_IDX = 4;

    public static Set<Activity> getCustomerActivitiesExpectingJobs(Plan plan) {
        Set<Activity> activitiesWithJob = new HashSet<>();
        plan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .filter(a -> activityExpectsCommercialJobs((Activity) a))
                .forEach(a -> activitiesWithJob.add((Activity) a));
        return activitiesWithJob;
    }

    public static Id<Carrier> getCurrentlySelectedCarrierForJob(Activity activity, int commercialJobIndex) {
        Collection<String> commercialJobProperties = getCommercialJob(activity, commercialJobIndex);
        return Id.create((String) commercialJobProperties.toArray()[COMMERCIALJOB_ATTRIBUTE_CARRIER_IDX], Carrier.class);
    }

    public static void setJobCarrier(Activity activity, int commercialJobIndex, Id<Carrier> carrier) {
        Collection<String> commercialJobProperties = getCommercialJob(activity, commercialJobIndex);
        List<String> copy = new ArrayList<>();
        copy.addAll(commercialJobProperties);
        copy.set(COMMERCIALJOB_ATTRIBUTE_CARRIER_IDX, carrier.toString());
        activity.getAttributes().putAttribute(COMMERCIALJOB_ATTRIBUTE_NAME + commercialJobIndex, copy);
    }

	public static void setJobDuration(Activity activity, int commercialJobIndex, double duration) {
		Collection<String> commercialJobProperties = getCommercialJob(activity, commercialJobIndex);
		List<String> copy = new ArrayList<>();
		copy.addAll(commercialJobProperties);
		copy.set(COMMERCIALJOB_ATTRIBUTE_DURATION_IDX, String.valueOf(duration));
		activity.getAttributes().putAttribute(COMMERCIALJOB_ATTRIBUTE_NAME + commercialJobIndex, copy);
	}

    public static Set<Id<Carrier>> getExistingOperatorsForMarket(Carriers carriers, String market) {
        return carriers.getCarriers().values().
                stream()
                .filter(carrier -> carrier.getAttributes().getAttribute(CARRIER_MARKET_ATTRIBUTE_NAME).equals(market))
                .map(Carrier::getId).
                collect(Collectors.toSet());
    }

    public static Id<Person> generateDriverId(Carrier carrier, CarrierVehicle carrierVehicle, int nextId) {
        return Id.createPersonId(FREIGHT_DRIVER_PREFIX + "_" + carrier.getId() + "_veh_" + carrierVehicle.getId() + "_" + nextId);
    }

    static Id<Carrier> getCarrierIdFromDriver(Id<Person> personId) {
        String idStr = personId.toString();
        String subStr = idStr.substring(FREIGHT_DRIVER_PREFIX.length() + 1, idStr.indexOf("_veh"));
        return Id.create(subStr, Carrier.class);
    }

    public static boolean planExpectsCommercialJobs(Plan plan) {
        return plan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .anyMatch(planElement -> activityExpectsCommercialJobs((Activity) planElement));
    }

    private static boolean activityExpectsCommercialJobs(Activity activity){
        for (String attKey : activity.getAttributes().getAsMap().keySet()) {
            if (attKey.startsWith(COMMERCIALJOB_ATTRIBUTE_NAME)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String,Object> getCommercialJobAttributes(Activity activity){
        Map<String,Object> commercialJobs = new HashMap<>();
        activity.getAttributes().getAsMap().forEach((key, value) -> {
            if (key.startsWith(COMMERCIALJOB_ATTRIBUTE_NAME)) {
                if (((Collection)value).size() != 5)
                    throw new IllegalArgumentException("wrong length of commercialJob attribute for activity=" + activity);
                commercialJobs.put(key, value);
            }
        });
        return commercialJobs;
    }

    public static int getNumberOfJobsForActivity(Activity activity){
        return getCommercialJobAttributes(activity).size();
    }

    public static String getCarrierMarket(Carrier carrier) {
        return (String) carrier.getAttributes().getAttribute(CARRIER_MARKET_ATTRIBUTE_NAME);
    }

    public static Map<String, Set<Id<Carrier>>> splitCarriersByMarket(Carriers carriers) {
        Map<String, Set<Id<Carrier>>> carriersSplitByMarket = new HashMap<>();
        for (Id<Carrier> carrierId : carriers.getCarriers().keySet()) {
            String market = getCarrierMarket(carriers.getCarriers().get(carrierId));
            Set<Id<Carrier>> carriersForMarket = carriersSplitByMarket.getOrDefault(market, new HashSet<>());
            carriersForMarket.add(carrierId);
            carriersSplitByMarket.put(market, carriersForMarket);
        }
        return carriersSplitByMarket;
    }

    @Nullable
    public static Collection<String> getCommercialJob(Activity activity, int jobIndex){
        return (Collection<String>) activity.getAttributes().getAttribute(COMMERCIALJOB_ATTRIBUTE_NAME + jobIndex);
    }

    private static String convertPropertiesArrayToAttributeValue(String[] jobProperties){
        if (jobProperties.length != 5) throw new IllegalArgumentException("a commercialJob needs to have 5 properties");
        String propertiesString = "";
        for (String jobProperty : jobProperties) {
            propertiesString += jobProperty + ";";
        }
        return propertiesString.substring(0,propertiesString.length() - 1 ); //cut off the last semicolon
    }

    public static void addCustomerCommercialJobAttribute(Activity activity, Id<Carrier> carrier,
                                                         int amount, double earliestStart, double latestStart, double duration) {
        int commercialJobIndex = getNumberOfJobsForActivity(activity) + 1;
        List<String> jobProperties = List.of(carrier.toString(), String.valueOf(amount), String.valueOf(earliestStart), String.valueOf(latestStart), String.valueOf(duration));
        if (activity.getAttributes().getAsMap().containsKey(COMMERCIALJOB_ATTRIBUTE_NAME + commercialJobIndex)) throw new RuntimeException("job attribute nr " + commercialJobIndex + " already exists");
        activity.getAttributes().putAttribute(COMMERCIALJOB_ATTRIBUTE_NAME + commercialJobIndex, jobProperties);
    }

}
