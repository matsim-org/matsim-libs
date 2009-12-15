/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTreatmentRecorder.java
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

package playground.meisterk.phd.controler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import playground.meisterk.phd.replanning.PhDStrategyManager;

/**
 * TODO order of plan strategies is probably different in getStrategies and getpersonTreatment.keySet(), respectively. Fix that.  
 * 
 * @author meisterk
 *
 */
public class PersonTreatmentRecorder implements StartupListener, IterationEndsListener, ShutdownListener {

	private static final String FILENAME = "personTreatment.txt";
	private static final Logger log;
	protected static final NumberFormat differenceFormat;

	private PrintStream out;

	static {
		log = Logger.getLogger(PersonTreatmentRecorder.class);
		differenceFormat = NumberFormat.getInstance();
		differenceFormat.setMaximumFractionDigits(3);
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
	 */
	public void notifyStartup(StartupEvent event) {
		try {
			this.out = new PrintStream(org.matsim.core.controler.Controler.getOutputFilename(FILENAME));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Controler c = event.getControler();
		int memorySize = c.getConfig().strategy().getMaxAgentPlanMemorySize();

		out.print("#iter");
		for (PlanStrategy strategy : c.getStrategyManager().getStrategies()) {
			for (int rank=0; rank <= memorySize; rank++) {
				out.print("\t");
				out.print(strategy.toString() + Integer.toString(rank));
			}
		}
		for (PlanStrategy strategy : c.getStrategyManager().getStrategies()) {
			out.print("\t");
			out.print(strategy.toString() + "_diff");
		}
		for (PlanStrategy strategy : c.getStrategyManager().getStrategies()) {
			out.print("\t");
			out.print(strategy.toString() + "_expBeta");
		}
		out.println();

	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.ShutdownListener#notifyShutdown(org.matsim.core.controler.events.ShutdownEvent)
	 */
	public void notifyShutdown(ShutdownEvent event) {
		this.out.close();
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		Controler c = event.getControler();
		PhDStrategyManager phDStrategyManager = (PhDStrategyManager) c.getStrategyManager();
		Map<String, Set<Person>> personTreatment = phDStrategyManager.getPersonTreatment();
		int memorySize = c.getConfig().strategy().getMaxAgentPlanMemorySize();
		
		log.info("Writing results...");

		if (personTreatment.size() == 0) {
			return;
		}
		
		out.print(event.getIteration());
		out.print(this.getCountsString(personTreatment, memorySize));
		out.print(this.getScoreDifferencesString(personTreatment));
		out.print(this.getExpBetaSelectorString(personTreatment, c.getConfig().charyparNagelScoring()));
		out.println();
		
		log.info("Writing results...done.");

	}

	String getExpBetaSelectorString(
			Map<String, Set<Person>> personTreatment, CharyparNagelScoringConfigGroup charyparNagelScoringConfigGroup) {

		String str = new String();
		
		for (String strategyName : personTreatment.keySet()) {
			Frequency freq = new Frequency();
			
			Set<Person> persons = personTreatment.get(strategyName);
			for (Person person : persons) {
				boolean isBest = this.isSelectedPlanTheBestPlan(person, charyparNagelScoringConfigGroup);
				freq.addValue(isBest ? 1 : 0);
			}

			str = str.concat("\t");
			str = str.concat(differenceFormat.format(freq.getPct(1)));
			
		}
		
		return str;
		
	}

	String getCountsString(Map<String, Set<Person>> personTreatment, int memorySize) {
		
		String str = new String();
		
		TreeMap<String, Frequency> counts = new TreeMap<String, Frequency>();
		
		
		for (String strategyName : personTreatment.keySet()) {
			Frequency freq = new Frequency();
			counts.put(strategyName, freq);
			
			Set<Person> persons = personTreatment.get(strategyName);
			for (Person person : persons) {
				int rank = this.getRankOfSelectedPlan(person);
				freq.addValue(rank);
			}
		}

		for (String column : counts.keySet()) {
			for (int rank=0; rank <= memorySize; rank++) {
				str = str.concat("\t");
				str = str.concat(Long.toString(counts.get(column).getCount(rank)));
			}
		}

		return str;
		
	}

	String getScoreDifferencesString(Map<String, Set<Person>> personTreatment) {
		String str = new String();

		TreeMap<String, ResizableDoubleArray> differences = new TreeMap<String, ResizableDoubleArray>();

		for (String strategyName : personTreatment.keySet()) {
			ResizableDoubleArray diff = new ResizableDoubleArray();
			differences.put(strategyName, diff);
			
			Set<Person> persons = personTreatment.get(strategyName);
			for (Person person : persons) {
				int rank = this.getRankOfSelectedPlan(person);
				if (rank == 0) {
					Double scoreDiff = this.getAbsoluteScoreDifference(person);
					if (scoreDiff != null) {
						diff.addElement(scoreDiff.doubleValue());
					}
				}
			}
			
			str = str.concat("\t");
			str = str.concat(differenceFormat.format(StatUtils.mean(diff.getElements())));
			
		}
		
		return str;
		
	}

	int getRankOfSelectedPlan(Person person) {
		
		int rank = 0;

		Double newScore = person.getSelectedPlan().getScore();
		for (Plan plan : person.getPlans()) {
			if (!plan.isSelected()) {
				Double otherScore = plan.getScore();
				if (otherScore != null) {
					if (otherScore >= newScore) {
						rank++;
					}
				}
			}
		}
		
		return rank;
	}

	boolean isSelectedPlanTheBestPlan(Person person, CharyparNagelScoringConfigGroup charyparNagelScoringConfigGroup) {
		
		ExpBetaPlanSelector expBetaPlanSelector = new ExpBetaPlanSelector(charyparNagelScoringConfigGroup);
		
		Plan thePlanItWouldSelect = expBetaPlanSelector.selectPlan(person);
		return (thePlanItWouldSelect == person.getSelectedPlan());
//		return (this.getRankOfSelectedPlan(person) == 0);
		
	}
	
	/**
	 * @param person
	 * @return the difference of the score of the selected plan to the score of the next worse plan
	 */
	Double getAbsoluteScoreDifference(Person person) {
	
		Double absoluteScoreDifference = null;
		
		Double selectedScore = person.getSelectedPlan().getScore();
		Double nextWorseScore = null;
		for (Plan plan : person.getPlans()) {
			if (!plan.isSelected()) {
				Double otherScore = plan.getScore();
				if (otherScore < selectedScore) {
					if (nextWorseScore == null) {
						nextWorseScore = otherScore;
					} else {
						if (otherScore > nextWorseScore) {
							nextWorseScore = otherScore;
						}
					}
				}
			}
		}
		
		if (nextWorseScore != null) {
			absoluteScoreDifference = selectedScore - nextWorseScore;
		}
		
		return absoluteScoreDifference;
		
	}
	
	private String getColumnName(String strategyName, int rank) {
		return strategyName + "_" + Integer.toString(rank);
	}

}
