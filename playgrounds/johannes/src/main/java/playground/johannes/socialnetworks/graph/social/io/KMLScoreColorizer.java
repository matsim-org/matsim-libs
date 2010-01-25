/* *********************************************************************** *
 * project: org.matsim.*
 * KMLScoreColorizer.java
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
package playground.johannes.socialnetworks.graph.social.io;

import gnu.trove.TDoubleObjectHashMap;

import java.util.HashMap;
import java.util.Map;

import net.opengis.kml._2.LinkType;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle;
import playground.johannes.socialnetworks.sim.SimSocialGraph;
import playground.johannes.socialnetworks.sim.SimSocialVertex;

/**
 * @author illenberger
 *
 */
public class KMLScoreColorizer extends KMLVertexColorStyle<SimSocialGraph, SimSocialVertex> {

	private Map<SimSocialVertex, String> styleIds = new HashMap<SimSocialVertex, String>();
	
//	private Distribution distribution = new Distribution();
	
	private double minValue = - Double.MAX_VALUE;
	
	private double maxValue = Double.MAX_VALUE;
	
	private double descretization = 1.0;
	
	public KMLScoreColorizer(LinkType vertexIconLink) {
		super(vertexIconLink);
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getDescretization() {
		return descretization;
	}

	public void setDescretization(double descretization) {
		this.descretization = descretization;
	}

	@Override
	protected TDoubleObjectHashMap<String> getValues(SimSocialGraph graph) {
		TDoubleObjectHashMap<String> values = new TDoubleObjectHashMap<String>();
	
		for(SimSocialVertex ego : graph.getVertices()) {
			Person person = ego.getPerson().getPerson();
			double sum = 0;
			for(Plan plan : person.getPlans()) {
				Double score = plan.getScore();
				if(score != null)
					sum += score; 	
			}
			double avr = sum/person.getPlans().size();
			avr = Math.min(avr, maxValue);
			avr = Math.max(avr, minValue);
			avr = Math.floor(avr/descretization) * descretization;
			values.put(avr, String.valueOf(avr));
			
			styleIds.put(ego, String.valueOf(avr));
			
//			distribution.add(val);
		}
		
		return values;
	}

	@Override
	public String getObjectSytleId(SimSocialVertex object) {
		return styleIds.get(object);
	}
	
//	public static void main(String args[]) throws IOException {
//		Config config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
//		ScenarioLoader loader = new ScenarioLoader(config);
//		loader.loadScenario();
//		ScenarioImpl scenario = loader.getScenario();
//		PopulationImpl population = scenario.getPopulation();
//		
//		SocialNetwork<Person> socialnet = new SocialNetwork<Person>(population);
//		KMLWriter writer = new KMLWriter();
//		writer.setDrawEdges(false);
//		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
//		KMLScoreColorizer colorizer = new KMLScoreColorizer(writer.getVertexIconLink());
//		colorizer.setMaxValue(250);
//		colorizer.setMinValue(100);
//		writer.setVertexStyle(colorizer);
//		writer.write(socialnet, "/Users/fearonni/vsp-work/runs-svn/run669/scores.kmz");
//		
////		Distribution.writeHistogram(colorizer.distribution.absoluteDistribution(), "/Users/fearonni/vsp-work/runs-svn/run669/sorces.hist");
//	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle#getValue(playground.johannes.socialnetworks.graph.Vertex)
	 */
	@Override
	protected double getValue(SimSocialVertex vertex) {
		// TODO Auto-generated method stub
		return 0;
	}
}
