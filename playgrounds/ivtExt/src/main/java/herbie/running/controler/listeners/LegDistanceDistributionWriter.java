/* *********************************************************************** *
 * project: org.matsim.*
 * LegDistanceDistributionWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.controler.listeners;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;
import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis.CrosstabFormat;
import herbie.running.population.algorithms.PopulationLegDistanceDistribution;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class LegDistanceDistributionWriter implements IterationEndsListener {

	public static final double[] distanceClasses = new double[]{
		0.0, 
		1000, 2000, 3000, 4000, 5000, 
		10000, 20000, 30000, 40000, 50000, 
		100000, 200000, 300000, 400000, 500000,
		1000000, Double.MAX_VALUE};

	private final String filename;
	private final Population subPopulation;

	private Network network;
	
	private final static Logger log = Logger.getLogger(LegDistanceDistributionWriter.class);

	public LegDistanceDistributionWriter(String filename, Network network) {
		this(filename, network, null);
	}

	/**
	 * One can use this constructor to analyze only a subset of the entire population.
	 */
	public LegDistanceDistributionWriter(String filename, Network network, Population subPopulation) {
		super();
		this.network = network;
		this.filename = filename;
		this.subPopulation = subPopulation;
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		if (event.getIteration() % 10 == 0) {

			Population pop;
			if (this.subPopulation == null) {
				MatsimServices c = event.getServices();
                pop = c.getScenario().getPopulation();
			} else pop = this.subPopulation;
			
			PrintStream out = null;
			try {
				out = new PrintStream(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), filename));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			AbstractClassifiedFrequencyAnalysis algo = new PopulationLegDistanceDistribution(out, network);
			
			algo.run(pop);
			
			log.info("Writing results file...");
			algo.printClasses(CrosstabFormat.ABSOLUTE, false, distanceClasses, out);
			algo.printDeciles(true, out);
			out.close();
			log.info("Writing results file...done.");

		}
		
	}

}
