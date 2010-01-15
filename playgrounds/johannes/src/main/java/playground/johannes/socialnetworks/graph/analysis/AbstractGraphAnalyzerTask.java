/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractGraphAnalyzerTast.java
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
package playground.johannes.socialnetworks.graph.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.contrib.sna.math.Distribution;


/**
 * @author illenberger
 *
 */
public abstract class AbstractGraphAnalyzerTask implements GraphAnalyzerTask {

	private String output;
	
	public AbstractGraphAnalyzerTask(String output) {
		this.output = output;
	}
	
	protected String getOutputDirectory() {
		return output;
	}
	
	protected void writeHistograms(Distribution distr, double binsize, boolean log, String name) throws FileNotFoundException, IOException {
		Distribution.writeHistogram(distr.absoluteDistribution(binsize), String.format("%1$s/%2$s.txt", output, name));
		Distribution.writeHistogram(distr.normalizedDistribution(binsize), String.format("%1$s/%2$s.share.txt", output, name));
		
		if(log) {
			Distribution.writeHistogram(distr.absoluteDistributionLog10(binsize), String.format("%1$s/%2$s.log10.txt", output, name));
			Distribution.writeHistogram(distr.absoluteDistributionLog2(binsize), String.format("%1$s/%2$s.log2.txt", output, name));
		
			Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog10(binsize)), String.format("%1$s/%2$s.share.log10.txt", output, name));
			Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog2(binsize)), String.format("%1$s/%2$s.share.log2.txt", output, name));
		}
	}
}
