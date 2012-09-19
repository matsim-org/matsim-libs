package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

public class Insertion {
	
	public ServiceProviderAgent getTourAgent() {
		return agent;
	}

	public InsertionData getInsertionData() {
		return insertionData;
	}

	private ServiceProviderAgent agent;
	
	private InsertionData insertionData;

	public Insertion(ServiceProviderAgent agent, InsertionData insertionData) {
		super();
		this.agent = agent;
		this.insertionData = insertionData;
	}
	
	

}
