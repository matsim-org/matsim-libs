/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.dgrether.analysis.population;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.households.Income;

/**
 * @author dgrether
 *
 */
public class DgPersonData {

	private static final Logger log = Logger.getLogger(DgPersonData.class);

	private Activity homeActivity;

	private Map<Id, DgPlanData> planData;

	private Id personId;

	private Income income;
	
	private Double toll = null;

	public DgPersonData() {
		this.planData = new HashMap<Id, DgPlanData>();
	}

/**
	 *
	 * @return the home location
	 */
	
	public void setToll(Double toll) {
		this.toll = toll;
	}

	public Double getToll() {
		return toll;
	}
	
	public Activity getFirstActivity() {
		return homeActivity;
	}

	public void setFirstActivity(Activity a) {
		this.homeActivity = a;
	}


	public Map<Id, DgPlanData> getPlanData() {
		return planData;
	}

	public Id getPersonId() {
		return personId;
	}


	public void setPersonId(Id personId) {
		this.personId = personId;
	}

	public void setIncome(Income income) {
		this.income = income;
	}

	public Income getIncome() {
		return this.income;
	}

	/**
	 * Score difference plan runid 2 - runid 1
	 */
	public double getDeltaScore(Id runId1, Id runId2) {
		DgPlanData plan1 = this.planData.get(runId1);
		DgPlanData plan2 = this.planData.get(runId2);
		if ((plan1 != null) && (plan2 != null)) {
			return plan2.getScore() - plan1.getScore();
		}
		else if (plan1 == null) {
			log.error("Person id " + personId + " has no plan (null) for runId " + runId1);
		}
		else if (plan2 == null) {
			log.error("Person id " + personId + " has no plan (null) for runId " + runId2);
		}
		return 0.0;
	}

}