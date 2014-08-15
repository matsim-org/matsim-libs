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

package playground.johannes.gsv.synPop.sim2;

import java.util.Collection;

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class BlockingSamplerListener implements SamplerListener {

	private long iterCounter;
	
	private final long interval;
	
	private final Sampler sampler;
	
	private final SamplerListener delegate;
	
	public BlockingSamplerListener(SamplerListener delegate, Sampler sampler, int interval) {
		this.delegate = delegate;
		this.sampler = sampler;
		this.interval = interval;
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson person) {
		iterCounter++;
		if((iterCounter + sampler.getNumThreads()) % interval == 0) {
			sampler.pause();
			delegate.afterStep(population, person);
			sampler.resume();
		}
	}
}
