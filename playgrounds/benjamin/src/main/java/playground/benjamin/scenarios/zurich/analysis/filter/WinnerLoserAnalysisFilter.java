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


package playground.benjamin.scenarios.zurich.analysis.filter;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

import playground.benjamin.scenarios.zurich.analysis.WinnerLoserAnalysis;
import playground.benjamin.scenarios.zurich.analysis.WinnerLoserAnalysisRow;


/**
 * @author benkick and fuerbas
 */

public class WinnerLoserAnalysisFilter extends WinnerLoserAnalysis {

	public WinnerLoserAnalysisFilter() {
	}

	public SortedMap<Id, WinnerLoserAnalysisRow> getWinner(SortedMap<Id, WinnerLoserAnalysisRow> populationInformation) {
		SortedMap<Id, WinnerLoserAnalysisRow> winner = new TreeMap<Id, WinnerLoserAnalysisRow>(new ComparatorImplementation());
		for (WinnerLoserAnalysisRow winnerLoserAnalysisRow : populationInformation.values()) {
			if (winnerLoserAnalysisRow.getScoreDiff() >= 0) {
				winner.put(winnerLoserAnalysisRow.getId(), winnerLoserAnalysisRow);
			}
		}
		return winner;
	}

	public SortedMap<Id, WinnerLoserAnalysisRow> getLoser(SortedMap<Id, WinnerLoserAnalysisRow> populationInformation) {
		SortedMap<Id, WinnerLoserAnalysisRow> loser = new TreeMap<Id, WinnerLoserAnalysisRow>(new ComparatorImplementation());
		for (WinnerLoserAnalysisRow winnerLoserAnalysisRow : populationInformation.values()) {
			if (winnerLoserAnalysisRow.getScoreDiff() < 0) {
				loser.put(winnerLoserAnalysisRow.getId(), winnerLoserAnalysisRow);
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