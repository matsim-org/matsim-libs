/* *********************************************************************** *
 * project: org.matsim.*
 * SamplerListenerComposite.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball2.sim;

import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;
import playground.johannes.socialnetworks.utils.Composite;

/**
 * @author illenberger
 *
 */
public class SamplerListenerComposite extends Composite<SamplerListener> implements SamplerListener {
	
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		boolean result = true;
		for(SamplerListener listener : components) {
			if(!listener.beforeSampling(sampler, vertex))
				result = false;
		}
		return result;
	}

	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		boolean result = true;
		for(SamplerListener listener : components) {
			if(!listener.afterSampling(sampler, vertex))
				result = false;
		}
		return result;
	}


	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		for(SamplerListener listener : components) {
			listener.endSampling(sampler);
		}
	}

}
