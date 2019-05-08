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
import org.matsim.contrib.freight.carrier.Carrier;

public class PersonDelivery {

    public static final String DELIEVERY_SIZE = "deliveryAmount";
    public static final String DELIEVERY_TYPE = "deliveryType";
    public static final String DELIEVERY_DURATION = "deliveryDuration";
    public static final String DELIEVERY_TIME_START = "deliveryTimeStart";
    public static final String SERVICE_OPERATOR = "operator";
    public static final String DELIEVERY_TIME_END = "deliveryTimeEnd";


    public static Id<Carrier> getCarrierId(Activity activity) {
        return Id.create(activity.getAttributes().getAttribute(PersonDelivery.DELIEVERY_TYPE).toString() + "_" + activity.getAttributes().getAttribute(PersonDelivery.SERVICE_OPERATOR).toString(), Carrier.class);
    }
}
