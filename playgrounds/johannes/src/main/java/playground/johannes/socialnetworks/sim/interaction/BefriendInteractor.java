/* *********************************************************************** *
 * project: org.matsim.*
 * BefriendInteractor.java
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
package playground.johannes.socialnetworks.sim.interaction;

import gnu.trove.TObjectIntHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.sna.graph.GraphUtils;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class BefriendInteractor implements Interactor {//, IterationStartsListener {

	private SocialSparseGraph socialnet;
	
	private SocialSparseGraphBuilder builder;
	
	private double tieProba;
	
	private Random random;
	
//	private int currentIteration;
	
	private Map<Id, SocialSparseVertex> vertices;
	
	private TObjectIntHashMap<String> actTypes;
	
	public BefriendInteractor(SocialSparseGraph socialnet, SocialSparseGraphBuilder builder, double p, long randomSeed) {
		this.socialnet = socialnet;
		this.builder = builder;
		this.tieProba = p;
		random = new Random(randomSeed);
		
		vertices = new HashMap<Id, SocialSparseVertex>(socialnet.getVertices().size());
		for(SocialSparseVertex vertex : socialnet.getVertices()) {
			vertices.put(vertex.getPerson().getId(), vertex);
		}
		
		actTypes = new TObjectIntHashMap<String>();
	}
	
	public void interact(Id p1, Id p2, double startTime, double endTime, String actType) {
		SocialSparseVertex v1 = vertices.get(p1);
//		Ego<Person> e1 = socialnet.getEgo(p1);
		SocialSparseVertex v2 = vertices.get(p2);
//		Ego<Person> e2 = socialnet.getEgo(p2);

		SocialEdge tie = (SocialEdge) GraphUtils.findEdge(v1, v2);
		if(tie == null) {
			/*
			 * Create tie...
			 */
			if(random.nextDouble() <= tieProba) {
				tie = builder.addEdge(socialnet, v1, v2);
				actTypes.adjustOrPutValue(actType, 1, 1);
			}
		} else {
			/*
			 * Reinforce tie...
			 */
//			tie.use(currentIteration);
		}
	}

	public TObjectIntHashMap<String> getActTypes() {
		return actTypes;
	}
//	public void notifyIterationStarts(IterationStartsEvent event) {
//		currentIteration = event.getIteration();
//	}

}
