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

package playground.johannes.gsv.synPop.sim3;

import java.util.Collection;

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class BlockingSamplerListener implements SamplerListener {

	private long iters;
	
	private final long interval;
	
	private long next;
	
	private final SamplerListener delegate;
	
	public BlockingSamplerListener(SamplerListener delegate, long interval) {
		this.delegate = delegate;
		this.interval = interval;
		this.next = interval;
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, Collection<ProxyPerson> mutations, boolean accept) {
		iters++;
		if(iters >= next) {
			synchronized(this) {
				if(iters >= next) {
					delegate.afterStep(population, mutations, accept);
					next += interval;
				}
			}
		}
	}
}
