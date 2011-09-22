/* *********************************************************************** *
 * project: org.matsim.*
 * CentralityTask.java
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
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class CentralityTask extends ModuleAnalyzerTask<Centrality> {

	private static final Logger logger = Logger.getLogger(CentralityTask.class);
	
	public static final String CLOSENESS = "closeness"; 
	
	public static final String BETWEENNESS = "betweenness";
	
	public static final String DIAMETER = "diameter";
	
	public static final String RADIUS = "radius";
	
	private boolean calcBetweenness = true;
	
	private boolean calcAPLDistribution = true;
	
	public CentralityTask() {
		setModule(new Centrality());
	}
	
	public void setCalcBetweenness(boolean calcBetweenness) {
		this.calcBetweenness = calcBetweenness;
	}

	public void setCalcAPLDistribution(boolean calcAPLDistribution) {
		this.calcAPLDistribution = calcAPLDistribution;
	}

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		module.init(graph, calcBetweenness, calcAPLDistribution);
		
		DescriptiveStatistics cDistr = module.closenessDistribution();
		statsMap.put(CLOSENESS, cDistr);
		printStats(cDistr, CLOSENESS);
		
		DescriptiveStatistics bDistr = module.vertexBetweennessDistribution();
		statsMap.put(BETWEENNESS, bDistr);
		printStats(bDistr, BETWEENNESS);
		
		statsMap.put("apl", module.getAPL());
		printStats(module.getAPL(), "apl");
		singleValueStats(DIAMETER, module.diameter(), statsMap);
		singleValueStats(RADIUS, new Double(module.radius()), statsMap);
		logger.info(String.format("diameter = %1$s, radius = %2$s", module.diameter(), module.radius()));
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(cDistr, "close", 100, 100);
				writeHistograms(bDistr, "between", 100, 100);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
