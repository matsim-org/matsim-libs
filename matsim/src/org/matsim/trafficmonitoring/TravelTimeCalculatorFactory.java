/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.trafficmonitoring;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.matsim.network.Link;

public class TravelTimeCalculatorFactory {
	
	private Constructor<? extends TravelTimeRole> prototypeContructorTravelTimeRole;
	private static final Class[] PROTOTYPECONSTRUCTOR_TRAVELTIMEROLE = {Link.class, int.class};

	private Constructor<? extends AbstractTravelTimeAggregator> prototypeContructorTravelTimeAggregator;
	private static final Class[] PROTOTYPECONSTRUCTOR_TRAVELTIMEAGGREGATOR = {int.class, int.class};
	
	public TravelTimeCalculatorFactory() {
		try {
			this.prototypeContructorTravelTimeRole = TravelTimeRoleArray.class
			.getConstructor(PROTOTYPECONSTRUCTOR_TRAVELTIMEROLE);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		
		try {
			this.prototypeContructorTravelTimeAggregator = OptimisticTravelTimeAggregator.class
			.getConstructor(PROTOTYPECONSTRUCTOR_TRAVELTIMEAGGREGATOR);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public void setTravelTimeRolePrototype(Class < ? extends TravelTimeRole> protoype) {
		Constructor<? extends TravelTimeRole> c;
		try {
			c = protoype.getConstructor(PROTOTYPECONSTRUCTOR_TRAVELTIMEROLE);
			if (c != null ) {
				this.prototypeContructorTravelTimeRole = c;
			} else {
				throw new IllegalArgumentException(
						"Wrong prototype constructor!");
			}
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTravelTimeAggregatorPrototype(Class <? extends AbstractTravelTimeAggregator> prototype) {
		Constructor <? extends AbstractTravelTimeAggregator> c;
	
		try {
			c = prototype.getConstructor(PROTOTYPECONSTRUCTOR_TRAVELTIMEAGGREGATOR);
			if (c != null) {
				this.prototypeContructorTravelTimeAggregator = c;
			} else {
				throw new IllegalArgumentException(
						"Wrong prototype constructor!");				
			}
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public AbstractTravelTimeAggregator createTravelTimeAggregator(int numSlots, int travelTimeBinSize) {
		AbstractTravelTimeAggregator ret;
		try {
			ret = this.prototypeContructorTravelTimeAggregator.newInstance(new Object[] {numSlots, travelTimeBinSize});
			return ret;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	public TravelTimeRole createTravelTimeRole(Link link, int numSlots) {
		TravelTimeRole ret;
		try {
			ret = this.prototypeContructorTravelTimeRole.newInstance(new Object[] {link, numSlots});
			return ret;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
