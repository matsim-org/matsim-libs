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

import gnu.trove.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 *
 */
public class ErrorTargetDistanceLogger implements SamplerListener {

	private final long logInterval;
	
	private final AtomicLong iter;
	
	private final Hamiltonian h;
	
	private final String outdir;
	
	public ErrorTargetDistanceLogger(Hamiltonian h, long logInterval, String outdir) {
		this.logInterval = logInterval;
		this.iter = new AtomicLong();
		this.h = h;
		this.outdir = outdir;
	}
	
	@Override
	public void afterStep(Collection<? extends Person> population, Collection<? extends Person> mutations, boolean accepted) {
		if(iter.get() % logInterval == 0) {
			long iterNow = iter.get();
			double[] err = new double[population.size()];
			double[] dist = new double[population.size()];
			int i = 0;
			for(Person person : population) {
				err[i] = h.evaluate(person);
				double sum = 0;
				for(Attributable leg : person.getEpisodes().get(0).getLegs()) {
					String val = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
					if(val != null) {
						sum += Double.parseDouble(val);
					}
				}
				dist[i] = sum;
				i++;
			}
			
			TDoubleDoubleHashMap hist = Correlations.mean(dist, err, FixedSampleSizeDiscretizer.create(dist, 100, 100));
			try {
				StatsWriter.writeHistogram(hist, "distance", "error", String.format("%s/%s.errorDistance.txt", outdir, iterNow));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		iter.getAndIncrement();
	}

}
