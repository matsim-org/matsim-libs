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
package playground.johannes.sna.snowball.sim;

import playground.johannes.sna.snowball.SampledVertexDecorator;
import playground.johannes.sna.util.Composite;

/**
 * A composite of multiple sampler listeners.
 * 
 * @author illenberger
 * 
 */
public class SamplerListenerComposite extends Composite<SamplerListener> implements SamplerListener {

	/**
	 * @see {@link SamplerListener#beforeSampling(Sampler, SampledVertexDecorator)}
	 */
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		boolean result = true;
		for (SamplerListener listener : components) {
			if (!listener.beforeSampling(sampler, vertex))
				result = false;
		}
		return result;
	}

	/**
	 * @see {@link SamplerListener#afterSampling(Sampler, SampledVertexDecorator)}
	 */
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		boolean result = true;
		for (SamplerListener listener : components) {
			if (!listener.afterSampling(sampler, vertex))
				result = false;
		}
		return result;
	}

	/**
	 * @see {@link SamplerListener#endSampling(Sampler)}
	 */
	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		for (SamplerListener listener : components) {
			listener.endSampling(sampler);
		}
	}

}
