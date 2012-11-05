package org.matsim.contrib.freight.vrp.algorithms.rr.recreate;

import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.contrib.freight.vrp.basics.InsertionData;

class Insertion {
	
	public RouteAgent getTourAgent() {
		return agent;
	}

	public InsertionData getInsertionData() {
		return insertionData;
	}

	private RouteAgent agent;
	
	private InsertionData insertionData;

	Insertion(RouteAgent agent, InsertionData insertionData) {
		super();
		this.agent = agent;
		this.insertionData = insertionData;
	}
	
	

}
