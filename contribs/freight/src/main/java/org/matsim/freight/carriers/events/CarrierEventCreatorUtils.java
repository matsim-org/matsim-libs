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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utils for {@link CarrierEventCreator}s
 *
 * @author kturner
 */
public final class CarrierEventCreatorUtils {

	private CarrierEventCreatorUtils(){
	}

	/**
	 * @return a collection of the standard freightEvent creators
	 */
	public static Collection<CarrierEventCreator> getStandardEventCreators(){
		List<CarrierEventCreator> creators = new ArrayList<>();
		creators.add(new CarrierServiceEndEventCreator());
		creators.add(new CarrierServiceStartEventCreator());
		creators.add(new CarrierShipmentDeliveryStartEventCreator());
		creators.add(new CarrierShipmentDeliveryEndEventCreator());
		creators.add(new CarrierShipmentPickupStartEventCreator());
		creators.add(new CarrierShipmentPickupEndEventCreator());
		creators.add(new CarrierTourEndEventCreator());
		creators.add(new CarrierTourStartEventCreator());
		return creators;
	}

}
