/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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
package playground.jjoubert.projects.wasteCollection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/**
 *
 * @author jwjoubert
 */
public class MclarpifPlanStrategy implements PlanStrategy {
	private final Logger log = Logger.getLogger(MclarpifPlanStrategy.class);

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		// TODO Auto-generated method stub
		log.warn("running the MCLARPIF PlanStrategy");
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub
		log.warn("initialized the MCLARPIF PlanStrategy");
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		log.warn("finished the MCLARPIF PlanStrategy");
	}

}
