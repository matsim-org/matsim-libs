/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeLogitSelector.java
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

package air.pathsize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * select an existing Plan according to something similar to the Path Size Logit (e.g. Frejinger, E. and Bierlaire, M.: Capturing Correlation
 * with subnetworks in route choice models, Transportation Research Part B (2006), doi:10.1016/j.trb.2006.06.003.)
 *
 * @author dgrether
 */
public final class PathSizeLogitSelector implements PlanSelector {
	
	private static final Logger log = Logger.getLogger(PathSizeLogitSelector.class);
	
	private final double pathSizeLogitExponent;
	private final double logitScaleFactor;
	/**
	 * Currently the mainModes must be specified explicitly. If the pt and car mode wouldn't be  kind of pseudo generic this would not be needed at all. 
	 */
	private Set<String> mainModes;
	
	/**
	 * I don't like pseudo generics so making explicit what is implicitly available: Two non generic modes that have 
	 * clearly a different behavior and need extra treatment: pt and car
	 */
	private PSLCalculator carPslCalculator = new CarPSLCalculator();
	private PSLCalculator ptPslCalculator = new PtPSLCalculator();
	private PSLCalculator genericPslCalculator = new GenericModePSLCalculator();
	private Map<String, PSLCalculator> pslCalculatorMap = new HashMap<String, PSLCalculator>();
	

	
	public PathSizeLogitSelector( final double pathSizeLogitExponent, final double logitScaleFactor, Set<String> mainModes) {
		this.pathSizeLogitExponent = pathSizeLogitExponent ;
		this.logitScaleFactor = logitScaleFactor ;
		this.mainModes = mainModes;
		log.info("Using main modes: " + mainModes);
	}
	
	@Override
	public Plan selectPlan(final HasPlansAndId<Plan, Person> person) {
		double maxScore = checkPlansScoreAndGetMaxScore(person);
		List<PSLPlanData> planDatasets = createPlanIdList(person.getPlans());
		Map<String, List<PSLPlanData>> plansByMainMode = sortPlansByMainMode(planDatasets);
		this.calcPSLValues(plansByMainMode);

		// - now calculate the weights, i.e. V_{in} + \beta_{PS} ln(PSL_{in})
		double sumWeights = 0.0;
		for (PSLPlanData plan : planDatasets) {
			double scorePlusCorrection = ((plan.getPlan().getScore() - maxScore) + (this.pathSizeLogitExponent * Math.log(plan.getPslValue())));
			plan.setWeight(Math.exp(this.logitScaleFactor * scorePlusCorrection));
			sumWeights += plan.getWeight();
		}
		// choose a random number over interval [0,sumWeights[
		double rndNum = sumWeights *MatsimRandom.getRandom().nextDouble();
		for (PSLPlanData plan : planDatasets) {
			if (plan.getWeight() < rndNum){
				return plan.getPlan();
			}
			rndNum += plan.getWeight();
		}
		// this case should never happen, except a person has no plans at all.
		return null;
	}
	
	private void calcPSLValues(Map<String, List<PSLPlanData>> plansByMainMode){
		for (Entry<String, List<PSLPlanData>> entry : plansByMainMode.entrySet()) {
			String mainMode = entry.getKey();
			if (entry.getValue().size() == 1) { // this is a shortcut - we don't have to consider plans that exist only once for a main mode as they get the psl value 1
				entry.getValue().get(0).setPslValue(1.0);
			}
			else {
				//first consider the pseudo generic modes
				if (mainMode == "pt") {
					this.ptPslCalculator.calculatePSLValues(entry.getValue());
				}
				else if (mainMode == "car") {
					this.carPslCalculator.calculatePSLValues(entry.getValue());
				}
				else if (this.pslCalculatorMap.containsKey(mainMode)) {
					this.pslCalculatorMap.get(mainMode).calculatePSLValues(entry.getValue());;
				}
				else { // all others
					this.genericPslCalculator.calculatePSLValues(entry.getValue());
				}
			}
		}
	}
	

	private Map<String, List<PSLPlanData>> sortPlansByMainMode(List<PSLPlanData> planIds) {
		Map<String, List<PSLPlanData>> m = new HashMap<String, List<PSLPlanData>>();
		for (PSLPlanData plan : planIds) {
			String mainMode = calcLegsOfMainMode(plan);
			plan.setMainMode(mainMode);
			List<PSLPlanData> list = m.get(mainMode);
			if (list == null){
				list = new ArrayList<PSLPlanData>();
				m.put(mainMode, list);
			}
			list.add(plan);
		}
		return m;
	}



	private String calcLegsOfMainMode(PSLPlanData plan) {
		String mainMode = null;
		for (PlanElement p : plan.getPlan().getPlanElements()) {
			if (p instanceof Leg) {
				Leg l = (Leg) p;
				String mode = l.getMode();
				if (this.mainModes.contains(mode)) {
					if (mainMode == null) {
						mainMode = mode;
					}
					else if (mainMode != mode) {
						throw new IllegalStateException("Found a plan that has more then one of the main modes within his legs. It is not clear how to handle this, in terms of concepts and implementation. Schedule time for further research and reimplement class...");
					}
					plan.getLegsOfMainMode().add(l);
				}
			}
		}
		return mainMode;
	}

	private List<PSLPlanData> createPlanIdList(List<? extends Plan> plans) {
		List<PSLPlanData> l = new ArrayList<PSLPlanData>();
		int id = 0;
		for (Plan p : plans){
			PSLPlanData pid = new PSLPlanData(id, p);
			l.add(pid);
			id++;
		}
		return l;
	}

	private double checkPlansScoreAndGetMaxScore(final HasPlansAndId<Plan, Person> person) {
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Plan plan : person.getPlans()){
			if (plan.getScore() == null || 
					Double.isInfinite(plan.getScore()) || 
					Double.isNaN(plan.getScore())) {
				log.debug("Person: " + person);
				log.debug("Plan: " + plan);
				throw new IllegalStateException("Too many unscored plans in choice set of person " + person.getId());
			}
			if (plan.getScore() > maxScore) {
				maxScore = plan.getScore();
			}
		}
		return maxScore;
	}

	public PSLCalculator getCarPslCalculator() {
		return carPslCalculator;
	}

	
	public void setCarPslCalculator(PSLCalculator carPslCalculator) {
		this.carPslCalculator = carPslCalculator;
	}

	
	public PSLCalculator getPtPslCalculator() {
		return ptPslCalculator;
	}

	
	public void setPtPslCalculator(PSLCalculator ptPslCalculator) {
		this.ptPslCalculator = ptPslCalculator;
	}

	
	public PSLCalculator getGenericPslCalculator() {
		return genericPslCalculator;
	}

	
	public void setGenericPslCalculator(PSLCalculator genericPslCalculator) {
		this.genericPslCalculator = genericPslCalculator;
	}

	
	public Map<String, PSLCalculator> getPslCalculatorMap() {
		return pslCalculatorMap;
	}


}
