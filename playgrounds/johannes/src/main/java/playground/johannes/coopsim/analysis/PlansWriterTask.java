/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterTask.java
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
package playground.johannes.coopsim.analysis;

import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

import playground.johannes.coopsim.LoggerUtils;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class PlansWriterTask extends TrajectoryAnalyzerTask {

	private final Network network;
	
	public PlansWriterTask(Network network) {
		this.network = network;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		LoggerUtils.setVerbose(false);
		Population population = new PopulationImpl(null);
		for(Trajectory t : trajectories) {
			population.addPerson(t.getPerson());
		}
		
		PopulationWriter writer = new PopulationWriter(population, network);
		writer.useCompression(true);
		writer.write(String.format("%1$s/plans.xml.gz", getOutputDirectory()));
		LoggerUtils.setVerbose(true);

	}

}
