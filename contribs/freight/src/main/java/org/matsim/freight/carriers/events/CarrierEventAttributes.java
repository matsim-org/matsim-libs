/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.events;

/**
 *  Some constants, that are used for the Attributes of different FreightEvents.
 *
 *  @author Kai Martins-Turner (kturner)
 */
public class CarrierEventAttributes {
	public static final String ATTRIBUTE_SERVICE_ID = "serviceId";
	public static final String ATTRIBUTE_SHIPMENT_ID = "shipmentId";
	public static final String ATTRIBUTE_TOUR_ID = "tourId";
	public static final String ATTRIBUTE_SERVICE_DURATION = "serviceDuration";
	public static final String ATTRIBUTE_PICKUP_DURATION = "pickupDuration";
	public static final String ATTRIBUTE_DROPOFF_DURATION = "dropoffDuration";
	public static final String ATTRIBUTE_CAPACITYDEMAND = "capacityDemand";
}
