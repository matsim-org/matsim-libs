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

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.io.XMLWriter;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 * 
 */
public class PopulationWriter implements SamplerListener {

	private static final Logger logger = Logger.getLogger(PopulationWriter.class);

	private final String outputDir;

	private final XMLWriter writer;

	private final long interval;

	private final AtomicLong iteration = new AtomicLong();

	public PopulationWriter(String outputDir, long interval) {
		this.outputDir = outputDir;
		this.interval = interval;
		writer = new XMLWriter();

	}

	@Override
	public void afterStep(Collection<? extends Person> population, Collection<? extends Person> mutations, boolean accepted) {
		if (iteration.get() % interval == 0) {
			/*
			 * The use of synchronized should be avoided by using a
			 * BlockinkSamplerListener, however, for some unknown reasons there
			 * are rare situation where this does not work.
			 */
			synchronized (this) {
				if (iteration.get() % interval == 0) {
				logger.info("Dumping population...");
				writer.write(String.format("%s/%s.pop.xml.gz", outputDir, iteration), population);
				logger.info("Done.");
				}
			}
		}
		iteration.incrementAndGet();
	}
}
