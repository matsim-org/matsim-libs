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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim2.SamplerListener;

/**
 * @author johannes
 *
 */
public class HamiltonianLogger implements SamplerListener {

	private static final Logger logger = Logger.getLogger(HamiltonianLogger.class);
	
	private final Hamiltonian h;
	
	private final int logInterval;
	
	private long iter;
	
	private BufferedWriter writer;
	
	private static final String TAB = "\t";
	
	public HamiltonianLogger(Hamiltonian h, int logInterval) {
		this(h, logInterval, null);
	}
	
	public HamiltonianLogger(Hamiltonian h, int logInterval, String file) {
		this.h = h;
		this.logInterval = logInterval;
		
		if(file != null) {
			try {
				writer = new BufferedWriter(new FileWriter(file));
				writer.write("iter\ttotal\tavr\tmed\tmin\tmax");
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson original, boolean accepted) {
		iter++;
		
		if(iter % logInterval == 0) {
			double[] values = new double[population.size()];
			int i = 0;
			for(ProxyPerson person : population) {
				values[i] = h.evaluate(person);
				i++;
			}
			DescriptiveStatistics stats = new DescriptiveStatistics(values);
			double sum = stats.getSum();
			double avr = stats.getMean();
			double med = stats.getPercentile(50);
			double max = stats.getMax();
			double min = stats.getMin();
			
			StringBuilder builder = new StringBuilder();
			builder.append("Statistics for ");
			builder.append(h.getClass().getSimpleName());
			builder.append(String.format(Locale.US, ": Sum = %.4f, ", sum));
			builder.append(String.format(Locale.US, ": Avr = %.4f, ", avr));
			builder.append(String.format(Locale.US, ": Med = %.4f, ", med));
			builder.append(String.format(Locale.US, ": Max = %.4f, ", max));
			builder.append(String.format(Locale.US, ": Min = %.4f", min));
			
			logger.info(builder.toString());
			
			if(writer != null) {
				try {
					writer.write(String.valueOf(iter));
					writer.write(TAB);
					writer.write(String.valueOf(sum));
					writer.write(TAB);
					writer.write(String.valueOf(avr));
					writer.write(TAB);
					writer.write(String.valueOf(med));
					writer.write(TAB);
					writer.write(String.valueOf(min));
					writer.write(TAB);
					writer.write(String.valueOf(max));
					writer.newLine();
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
