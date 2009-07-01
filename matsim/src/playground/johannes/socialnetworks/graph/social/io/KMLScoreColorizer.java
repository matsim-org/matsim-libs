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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.TDoubleObjectHashMap;
import net.opengis.kml._2.LinkType;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;

/**
 * @author illenberger
 *
 */
public class KMLScoreColorizer extends KMLVertexColorStyle<SocialNetwork<BasicPerson<?>>, Ego<BasicPerson<?>>> {

	private Map<Ego<?>, String> styleIds = new HashMap<Ego<?>, String>();
	
	public KMLScoreColorizer(LinkType vertexIconLink) {
		super(vertexIconLink);
	}

	@Override
	protected TDoubleObjectHashMap<String> getValues(SocialNetwork<BasicPerson<?>> graph) {
		TDoubleObjectHashMap<String> values = new TDoubleObjectHashMap<String>();
	
		for(Ego<BasicPerson<?>> ego : graph.getVertices()) {
			BasicPerson<?> person = ego.getPerson();
			double sum = 0;
			for(BasicPlan<?> plan : person.getPlans()) {
				Double score = plan.getScore();
				if(score != null)
					sum += score; 	
			}
			int val = (int) (sum/person.getPlans().size());
			values.put(val, String.valueOf(val));
			
			styleIds.put(ego, String.valueOf(val));
		}
		
		return values;
	}

	public String getObjectSytleId(Ego<BasicPerson<?>> object) {
		return styleIds.get(object);
	}
	
	public static void main(String args[]) throws IOException {
		Config config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		Scenario scenario = loader.getScenario();
		Population population = scenario.getPopulation();
		
		SocialNetwork<PersonImpl> socialnet = new SocialNetwork<PersonImpl>(population);
		KMLWriter writer = new KMLWriter();
		writer.setDrawEdges(false);
		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
		writer.setVertexStyle(new KMLScoreColorizer(writer.getVertexIconLink()));
		writer.write(socialnet, "/Users/fearonni/vsp-work/runs-svn/run669/scores.kmz");
	}
}
