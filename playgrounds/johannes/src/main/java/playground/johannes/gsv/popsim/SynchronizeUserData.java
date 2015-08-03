/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import playground.johannes.gsv.synPop.sim3.SamplerListener;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 * 
 */
public class SynchronizeUserData implements SamplerListener {

	private final long interval;

	private final AtomicLong iters;

	private final Map<Object, String> keys;

	public SynchronizeUserData(Map<Object, String> keys, long interval) {
		this.keys = keys;
		this.iters = new AtomicLong();
		this.interval = interval;
	}

	@Override
	public void afterStep(Collection<PlainPerson> population, Collection<PlainPerson> mutations, boolean accepted) {
		if (iters.get() % interval == 0) {
			for (Map.Entry<Object, String> keyPair : keys.entrySet()) {
				for (PlainPerson person : population) {
					Object value = person.getUserData(keyPair.getKey());
					if (value != null) {
						person.setAttribute(keyPair.getValue(), String.valueOf(value));
					} else {
						person.setAttribute(keyPair.getValue(), null);
					}
				}
			}
		}

		iters.incrementAndGet();
	}

}
