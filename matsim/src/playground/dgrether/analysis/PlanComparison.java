/* *********************************************************************** *
 * project: org.matsim.*
 * PlanComparison.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.dgrether.analysis;

import java.util.Hashtable;
import java.util.Set;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Act;
/**
 * This Class provides a data object to compare to iterations. It
 * is needed to save some memory while running the PlanComparator. 
 * @author dgrether
 * 
 */
public class PlanComparison {
	
	private Hashtable<Id, Triple> _table;
	/**
	 * Creates a PlanComparison Object with the initial size
	 * @param size
	 */
	public PlanComparison(int size) {
     _table = new Hashtable<Id, Triple>(size, 0.9f);
	
	}
	/**
	 * Add the data from the first population
	 * @param id
	 * @param score
	 * @param home
	 */
	public void addFirstPlansData(Id id, double score, Act home) {
		Triple t = new Triple(score, home);
		_table.put(id, t);
	}
	/**
	 * Add the data from the second population
	 * @param id
	 * @param score
	 */
	public void addSecondPlansData(Id id, double score) {
	  Triple t = _table.get(id);
	  t.setSecondScore(score);
	}
	
	/**
	 * Get all Ids of the population
	 * @return a Set with IdIs
	 */
	public Set<Id> getPersonIds() {
		return _table.keySet();
	}
	/**
	 * 
	 * @param personId
	 * @return the score of the first iteration
	 */
	public double getFirstScore(Id personId) {
		return _table.get(personId)._score1;
	}
	/**
	 * 
	 * @param personId
	 * @return the score of the second iteration
	 */
	public double getSecondScore(Id personId) {
		return _table.get(personId)._score2;
	}
	/**
	 * 
	 * @param personId
	 * @return the home location of the agent with the given IdI
	 */
	public Act getHomeLocation(Id personId) {
		return _table.get(personId)._act;
	}
	
	/**
	 * Inner Class to use the HashTable in an efficient way.
	 * @author dgrether
	 *
	 */
	private class Triple {
		/**
		 * 
		 */
		private double _score1;
		/**
		 * 
		 */
		private double _score2;
		/**
		 * 
		 */
		private Act _act;
		/**
		 * 
		 * @param score the score of the first plan
		 * @param a
		 */
		public Triple(double score, Act a) {
			_score1 = score;
			_act = a;
		}
		/**
		 * 
		 * @param score
		 */
		public void setSecondScore(double score) {
			_score2 = score;
		}
		/**
		 * 
		 * @return double
		 */
		public double getFirstScore() {
			return _score1;
		}
		/**
		 * 
		 * @return double
		 */
		public double getSecondScore() {
			return _score2;
		}
		/**
		 * 
		 * @return the home location
		 */
		public Act getAct() {
			return _act;
		}
		
	}
	

}
