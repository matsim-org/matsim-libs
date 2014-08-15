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

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim.SamplerListener;

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
				writer.write("iter\ttotal\tavr");
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson original, ProxyPerson mutation, boolean accepted) {
		iter++;
		
		if(iter % logInterval == 0) {
			double val = 0;//h.evaluate(population);
			double avr = val/(double)population.size();
			logger.info(String.format(Locale.US, "Score for %s: avr = %.4f, total = %s.", h.getClass().getSimpleName(), avr, val));
			
			if(writer != null) {
				try {
					writer.write(String.valueOf(iter));
					writer.write(TAB);
					writer.write(String.valueOf(val));
					writer.write(TAB);
					writer.write(String.valueOf(avr));
					writer.newLine();
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
