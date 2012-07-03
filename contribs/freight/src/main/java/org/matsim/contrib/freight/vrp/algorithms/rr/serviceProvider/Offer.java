package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

public class Offer {
	
	private ServiceProviderAgent serviceProvider;
	
	private double price;
	
	public Offer(ServiceProviderAgent serviceProvider, double price) {
		super();
		this.serviceProvider = serviceProvider;
		this.price = price;
	}

	public ServiceProviderAgent getServiceProvider() {
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