/* *********************************************************************** *
 * project: org.matsim.*
 * PajekDistanceColorizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnet.io;

import java.io.IOException;

import gnu.trove.TObjectDoubleHashMap;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.graph.io.PajekColorizer;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class PajekDistanceColorizer<P extends BasicPerson<? extends BasicPlan<? extends BasicPlanElement>>> extends PajekColorizer<Ego<P>, SocialTie> {

	private double d_min;
	
	private double d_max;
	
	private TObjectDoubleHashMap<Ego<P>> d_mean;
	
	private boolean logScale = false;
	
	public PajekDistanceColorizer(SocialNetwork<P> socialnet, boolean logScale) {
		super();
		setLogScale(logScale);
		d_mean = SocialNetworkStatistics.getMeanEdgeLength(socialnet);
		
		d_min = Double.MAX_VALUE;
		d_max = Double.MIN_VALUE;
		for(double value : d_mean.getValues()) {
			d_min = Math.min(value, d_min); 
			d_max = Math.max(value, d_max);
		}
	}
	
	public void setLogScale(boolean flag) {
		logScale = flag;
	}
	
	@Override
	public String getEdgeColor(SocialTie e) {
		return getColor(-1);
	}

	@Override
	public String getVertexFillColor(Ego<P> v) {
		double color = -1;
		if(logScale) {
			double min = Math.log(d_min + 1);
			double max = Math.log(d_max + 1);
			color = (Math.log(d_mean.get(v)+ 1) - min) / (max - min);
		} else {
			color = (d_mean.get(v) - d_min)/(d_max - d_min);
		}
		
		return getColor(color);
	}
	
	public static void main(String args[]) throws IOException {
		String MODULE_NAME = "gravityGenerator";
		
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioImpl data = new ScenarioImpl(config);
		Population population = data.getPopulation();
		
		String outputDir = config.getParam(MODULE_NAME, "output");
		/*
		 * Setup social network and adjacency matrix.
		 */
		SNGraphMLReader<Person> reader = new SNGraphMLReader<Person>(population);
		SocialNetwork<Person> socialnet = reader.readGraph(config.getParam(MODULE_NAME,"socialnetwork"));
		
		PajekDistanceColorizer<Person> colorizer1 = new PajekDistanceColorizer<Person>(socialnet, true);
		SNPajekWriter<Person> pwriter = new SNPajekWriter<Person>();
		pwriter.write(socialnet, colorizer1, outputDir + "socialnet.distance.net");
		
		WeightedStatistics stats = new WeightedStatistics();
		stats.addAll(SocialNetworkStatistics.getMeanEdgeLength(socialnet).getValues());
		WeightedStatistics.writeHistogram(stats.absoluteDistribution(1000), outputDir + "d_mean.hist.txt");
	}

}
