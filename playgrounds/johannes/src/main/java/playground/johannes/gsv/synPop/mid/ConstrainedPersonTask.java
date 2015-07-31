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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 *
 */
public class ConstrainedPersonTask implements ProxyPersonTask {

	private final String key;
	
	private final String value;
	
	private final ProxyPersonTask delegate;
	
	public ConstrainedPersonTask(String key, String value, ProxyPersonTask delegate) {
		this.key = key;
		this.value = value;
		this.delegate = delegate;
	}
	
	@Override
	public void apply(PlainPerson person) {
		String val = person.getAttribute(key);
		if(value.equals(val)) {
			delegate.apply(person);
		}

	}

}
