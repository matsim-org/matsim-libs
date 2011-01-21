/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractGibbsSampler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.mcmc;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public abstract class AbstractGibbsSampler {

	private static final Logger logger = Logger.getLogger(AbstractGibbsSampler.class);
	
	final protected Random random;
	
	private long logInterval = (long) 1E6;
	
	public AbstractGibbsSampler() {
		random = new Random();
	}
	
	public AbstractGibbsSampler(long seed) {
		random = new Random(seed);
	}
	
	public void setLogInterval(long interval) {
		this.logInterval = interval;
	}
	
	public <V extends Vertex> void sample(AdjacencyMatrix<V> y, EnsembleProbability p, SamplerListener<V> listener) {
		long time = System.currentTimeMillis();
		
		int accept = 0;
		long it = 0;
		while(listener.beforeSampling(y, it)) {
			it++;
			if(step(y, p))
				accept++;
			
			if(it % logInterval == 0) {
				logger.info(String.format("[%1$s] Accepted %2$s of %3$s steps (ratio = %4$.6f).", it, accept, logInterval, accept/(double)logInterval));
				accept = 0;
			}
		}
				
		logger.info(String.format("Sampling done in %1$s s.", (System.currentTimeMillis() - time)/1000));
	}
	
	abstract protected <V extends Vertex> boolean step(AdjacencyMatrix<V> m, EnsembleProbability p);
}
