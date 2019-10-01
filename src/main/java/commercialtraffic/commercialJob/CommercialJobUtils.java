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

public class CommercialJobUtils {

    public static final String COMMERCIALJOB_ATTRIBUTE_NAME = "commercialJob";
    static final String COMMERCIALJOB_ATTRIBUTE_DELIMITER = ";";
    static final String CARRIERSPLIT = "_";

    //the pattern for the activity attribute is the following:
    //<attribute name="commercialJob[NUMBER]" class="java.lang.String">[TYPE];[OPERATOR];[CAPACITYDEMAND];[EARLIESTSTART];[LATESTSTART];[DURATION]</attribute>
    static final int COMMERCIALJOB_ATTRIBUTE_DURATION_IDX = 5;
    static final int COMMERCIALJOB_ATTRIBUTE_TYPE_IDX = 0;
    static final int COMMERCIALJOB_ATTRIBUTE_OPERATOR_IDX = 1;
    static final int COMMERCIALJOB_ATTRIBUTE_AMOUNT_IDX = 2;
    static final int COMMERCIALJOB_ATTRIBUTE_START_IDX = 3;
    static final int COMMERCIALJOB_ATTRIBUTE_END_IDX = 4;

    static Set<Activity> getActivitiesWithJobs (Plan plan){
        Set<Activity> activitiesWithJob = new HashSet<>();
        plan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .filter(a -> activityExpectsCommercialJobs((Activity) a))
                .forEach(a -> activitiesWithJob.add((Activity) a));
        return activitiesWithJob;
    }


    public static Id<Carrier> getCurrentCarrierForJob(Activity activity, int commercialJobIndex) {
        String[] commercialJobProperties = String.valueOf(getCommercialJob(activity, commercialJobIndex)).split(COMMERCIALJOB_ATTRIBUTE_DELIMITER);
        String id = commercialJobProperties[COMMERCIALJOB_ATTRIBUTE_TYPE_IDX] + CARRIERSPLIT + commercialJobProperties[COMMERCIALJOB_ATTRIBUTE_OPERATOR_IDX];
        return Id.create(id, Carrier.class);
    }

    static void setJobOperator(Activity activity, int commercialJobIndex, String operator) {
        String[] commercialJobProperties = String.valueOf(getCommercialJob(activity, commercialJobIndex)).split(COMMERCIALJOB_ATTRIBUTE_DELIMITER);
        commercialJobProperties[COMMERCIALJOB_ATTRIBUTE_OPERATOR_IDX] = operator;
        activity.getAttributes().putAttribute(COMMERCIALJOB_ATTRIBUTE_NAME + commercialJobIndex, convertPropertiesArrayToAttributeValue(commercialJobProperties));
    }

    public static String getJobOperator(Activity activity, int commercialJobIndex) {
        return String.valueOf(getCommercialJob(activity, commercialJobIndex)).split(COMMERCIALJOB_ATTRIBUTE_DELIMITER)[COMMERCIALJOB_ATTRIBUTE_OPERATOR_IDX];
    }

    static String getJobType(Activity activity, int commercialJobIndex) {
        return String.valueOf(getCommercialJob(activity, commercialJobIndex)).split(COMMERCIALJOB_ATTRIBUTE_DELIMITER)[COMMERCIALJOB_ATTRIBUTE_TYPE_IDX];
    }

    static Set<Id<Carrier>> getExistingOperatorsForJobType(Carriers carriers, String jobType) {
        return carriers.getCarriers().values().
                stream().
                filter(carrier -> carrier.getId().toString().startsWith(jobType)).map(Carrier::getId).
                collect(Collectors.toSet());
    }

    public static Id<Carrier> getCarrierIdFromDriver(Id<Person> personId) {
        return Id.create(personId.toString().split(CARRIERSPLIT)[1] + CARRIERSPLIT + personId.toString().split(CARRIERSPLIT)[2], Carrier.class);
    }

    static boolean planExpectsCommercialJobs(Plan plan) {
        return plan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .anyMatch(planElement -> activityExpectsCommercialJobs((Activity) planElement));
    }

    private static boolean activityExpectsCommercialJobs(Activity activity){
        MutableBoolean activityExpectsCommercialJobs = new MutableBoolean(false);
        activity.getAttributes().getAsMap().keySet().forEach(k -> {
            if(k.startsWith(COMMERCIALJOB_ATTRIBUTE_NAME)) activityExpectsCommercialJobs.setTrue();
        });
        return activityExpectsCommercialJobs.getValue();
    }

    static Map<String,Object> getCommercialJobAttributes(Activity activity){
        Map<String,Object> commercialJobs = new HashMap<>();
        activity.getAttributes().getAsMap().forEach((key, value) -> {
            if (key.startsWith(COMMERCIALJOB_ATTRIBUTE_NAME)) commercialJobs.put(key, value);
        });
        return commercialJobs;
    }

    static int getNumberOfJobsForActivity(Activity activity){
        return getCommercialJobAttributes(activity).size();
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

    private static Object getCommercialJob(Activity activity, int jobIndex){
        return activity.getAttributes().getAttribute(COMMERCIALJOB_ATTRIBUTE_NAME + jobIndex);
    }

    private static String convertPropertiesArrayToAttributeValue(String[] jobProperties){
        if (jobProperties.length != 6) throw new IllegalArgumentException("a commercialJob needs to have 6 properties");
        String propertiesString = "";
        for (String jobProperty : jobProperties) {
            propertiesString += jobProperty + ";";
        }
        return propertiesString.substring(0,propertiesString.length() - 1 ); //cut off the last semicolon
    }

}
