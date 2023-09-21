package org.matsim.core.replanning.inheritance;

import java.util.ArrayList;
import java.util.List;

public class PlanInheritanceRecord {
	
	String agentId;
	String planId;
	String ancestorId;
	String mutatedBy;
	int iterationCreated;
	int iterationRemoved;
	
	/**
	 * Collection of iterations this plan had been the selected plan.
	 * Initialize this with one since each plan is selected at least once. 
	 */
	List<Integer> iterationsSelected = new ArrayList<>(1);

}
