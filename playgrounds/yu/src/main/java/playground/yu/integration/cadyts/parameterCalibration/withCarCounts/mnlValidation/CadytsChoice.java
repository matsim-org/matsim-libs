/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsChoice.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.scoring.Events2ScoreI;
import utilities.math.BasicStatistics;

public interface CadytsChoice extends Events2ScoreI {

	/** save Attr values into Maps */
	@Override
	public void finish();

	public PlanCalcScoreConfigGroup getScoring();

	public void reset(List<Tuple<Id, Plan>> toRemoves);

	public void setPersonAttrs(Person person, BasicStatistics[] statistics);

	public void setPersonScore(Person person);
}