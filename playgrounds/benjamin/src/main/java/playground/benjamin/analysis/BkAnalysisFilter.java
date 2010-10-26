/* *********************************************************************** *
 * project: org.matsim.*
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


package playground.benjamin.analysis;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;


/**
 * @author benkick and fuerbas
 */

public class BkAnalysisFilter extends BkAnalysis {

	BkAnalysisFilter() {
	}

	public SortedMap<Id, Row> getWinner(SortedMap<Id, Row> populationInformation) {
		SortedMap<Id, Row> winner = new TreeMap<Id, Row>(new ComparatorImplementation());
		for (Row row : populationInformation.values()) {
			if (row.getScoreDiff() >= 0) {
				winner.put(row.getId(), row);
			}
		}
		return winner;
	}

	public SortedMap<Id, Row> getLoser(SortedMap<Id, Row> populationInformation) {
		SortedMap<Id, Row> loser = new TreeMap<Id, Row>(new ComparatorImplementation());
		for (Row row : populationInformation.values()) {
			if (row.getScoreDiff() < 0) {
				loser.put(row.getId(), row);
			}
		}
		return loser;		
	}
	
//	protected SortedMap<Id, Row> changesMode (Population pop1, Population pop2) {
//		SortedMap<Id, Row> changes = new TreeMap<Id, Row>(new ComparatorImplementation());
//		for (Person person : pop1.getPersons().values()) {
//			Row row = new Row();
//			row.setId(person.getId());
//			row.setScore1();
//
//			for (Plan plan1 : person.getPlans()) {
//				for (PlanElement element1 : plan1.getPlanElements()){
//					if (element1 instanceof Leg) {
//						Plan plan2 = (Plan) pop2.getPersons().get(person.getId()).getPlans();
//						PlanElement element2 = (PlanElement) plan2.getPlanElements();
//						if (((Leg) element1).getMode() != ((Leg)element2).getMode())	{
//
//							changes.put(person.getId(), value)
//						}
//					}
//				}
//			}
//		}
//		return null;
//	}

}