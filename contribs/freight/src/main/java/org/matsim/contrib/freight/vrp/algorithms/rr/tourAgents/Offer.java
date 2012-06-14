package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

public class Offer {
	
	private ServiceProvider serviceProvider;
	
	private double price;
	
	public Offer(ServiceProvider serviceProvider, double price, double mc) {
		super();
		this.serviceProvider = serviceProvider;
		this.price = price;
	}

	public ServiceProvider getServiceProvider() {
		return serviceProvider;
	}

	public double getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return "currentTour=" + serviceProvider + "; marginalInsertionCosts=" + price;
	}
	
}