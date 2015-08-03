/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class ConstrainedPlanTask implements ProxyPlanTask {

	private final String key;
	
	private final String value;
	
	private final ProxyPlanTask delegate;
	
	public ConstrainedPlanTask(String key, String value, ProxyPlanTask delegate) {
		this.key = key;
		this.value = value;
		this.delegate = delegate;
	}
	
	@Override
	public void apply(Episode plan) {
		String val = plan.getAttribute(key);
		if(value.equals(val)) {
			delegate.apply(plan);
		}
	}

}
