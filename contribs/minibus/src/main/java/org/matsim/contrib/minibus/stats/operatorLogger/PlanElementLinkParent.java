/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.stats.operatorLogger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;

import java.util.*;

/**
 * Add a pointer from each child to its parent.
 * 
 * @author aneumann
 *
 */
public class PlanElementLinkParent {
	
	private static final Logger log = Logger.getLogger(PlanElementLinkParent.class);
	
	/**
	 * Add a pointer from each child to its parent.
	 */
	public static List<PlanElement> linkParentPlansToGivenPlanElements(List<PlanElement> planElements){
		
		Map<Id<Operator>, HashMap<Id<PPlan>, PlanElement>> operatorId2planId2planElement = new HashMap<Id<Operator>, HashMap<Id<PPlan>, PlanElement>>();
		
		// resort
		for (PlanElement planElement : planElements) {
			
			Id<Operator> operatorId = planElement.getOperatorId();
			if (operatorId2planId2planElement.get(operatorId) == null) {
				operatorId2planId2planElement.put(operatorId, new HashMap<Id<PPlan>, PlanElement>());
			}
			
			operatorId2planId2planElement.get(operatorId).put(planElement.getPlanId(), planElement);
		}
		
		// link parents
		for (PlanElement planElement : planElements) {
			try {
				HashMap<Id<PPlan>, PlanElement> plans = operatorId2planId2planElement.get(planElement.getOperatorId());
				PlanElement parent = plans.get(planElement.getParentId());
				planElement.setParentPlan(parent);
			} catch (Exception e) {
				log.error("Could not find parent plan element " + planElement.getParentId() + " for operator " + planElement.getOperatorId() + " plan " + planElement.getPlanId());
			}
		}
		
		return planElements;
	}
}
