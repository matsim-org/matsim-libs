package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;


public class Offer {
	
	private ServiceProvider agent;
	
	private double cost;

	public Offer(ServiceProvider agent, double cost) {
		super();
		this.agent = agent;
		this.cost = cost;
	}

	public ServiceProvider getServiceProvider() {
		return agent;
	}

	public double getPrice() {
		return cost;
	}
	
	@Override
	public String toString() {
		return "currentTour=" + agent + "; marginalInsertionCosts=" + cost;
	}
	
}