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

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.XMLWriter;

/**
 * @author johannes
 *
 */
public class PopulationWriter implements SamplerListener {
	
	private static final Logger logger = Logger.getLogger(PopulationWriter.class);

	private String outputDir;
	
	private XMLWriter writer;
	
	private int dumpInterval = 100000;
	
	private long iteration = 0;
	
	public PopulationWriter(String outputDir) {
		this.outputDir = outputDir;
		writer = new XMLWriter();
		
	}
	
	public void setDumpInterval(int interval) {
		dumpInterval = interval;
	}
	
	@Override
	public void afterStep(Collection<ProxyPerson> population, Collection<ProxyPerson> mutations, boolean accepted) {
		iteration++;
		if(iteration % dumpInterval == 0) {
			logger.info("Dumping population...");
			writer.write(String.format("%s/%s.pop.xml.gz", outputDir, iteration), population);
			logger.info("Done.");
		}
	}
}
