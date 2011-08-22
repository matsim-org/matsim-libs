/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSFAccumulator.java
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
package playground.johannes.socialnetworks.sim.interaction;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

/**
 * @author illenberger
 *
 */
public class SocialSFAccumulator extends ScoringFunctionAccumulator {

	private Person ego;
	
	private Set<Person> alters;
	
	private Map<Person, JointActivityScorer> functions;
	
	private ScoringMode mode;
	
	private double previousScore;
	
	private TObjectDoubleHashMap<Person> previousSocialScors;
	
	private double score;
	
	public SocialSFAccumulator(Set<Person> alters) {
		this.alters = alters;
		
		functions = new HashMap<Person, JointActivityScorer>(alters.size());
		previousSocialScors = new TObjectDoubleHashMap<Person>();
	}
	
	public void addScoringFunction(JointActivityScorer function, Person alter) {
		addScoringFunction(function);
		functions.put(alter, function);
	}
	
	public void setMode(ScoringMode mode) {
		this.mode = mode;
	}
	
	public void setEgo(Person ego) {
		this.ego = ego;
	}
	
	public double getScore() {
		return score;
	}

	public void finish() {
		super.finish();
		
		if (mode.equals(ScoringMode.UNAFFECTED)) {
			score = previousScore;
		} else {
			TObjectDoubleHashMap<Person> scores = getSocialScores();
			
			if (mode.equals(ScoringMode.ALTER)) {
				double delta = previousSocialScors.get(ego) - scores.get(ego);
				score = previousScore - delta;
				
			} else if (mode.equals(ScoringMode.EGO)){
				score = super.getScore();
				
			} else {
				throw new RuntimeException();
				
			}
			
			previousSocialScors = scores;
		}
	}
	
	private TObjectDoubleHashMap<Person> getSocialScores() {
		TObjectDoubleHashMap<Person> scores = new TObjectDoubleHashMap<Person>();
		for(Person alter : alters) {
			JointActivityScorer function = functions.get(alter);
			scores.put(alter, function.getScore());
		}
		
		return scores;
	}

	@Override
	public void reset() {
		super.reset();
		previousScore = score;
	}
	
	public int visitors() {
		int visitors = 0;
		for(JointActivityScorer scorer : functions.values()) {
			if(scorer.getScore() > 0) {
				visitors++;
			}
		}
		
		return visitors;
	}
	
	public double[] joinTimes() {
		TDoubleArrayList times = new TDoubleArrayList();
		for(JointActivityScorer scorer : functions.values()) {
			if(scorer.getScore() > 0) {
				times.add(scorer.getScore()/scorer.beta_join);
			}
		}
		
		return times.toNativeArray();
	}
	
	public double totalSocialScore() {
		TObjectDoubleHashMap<Person> scores = getSocialScores();
		TObjectDoubleIterator<Person> it = scores.iterator();
		double sum = 0;
		for(int i = 0; i < scores.size(); i++) {
			it.advance();
			sum += it.value();
		}
		return sum;
	}
}
