package playground.sebhoerl.avtaxi.refactor.aggregation;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.taxi.data.TaxiRequest;

import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;

public class AVAggregateTaxiRequest {
	final private List<TaxiRequest> requests = new LinkedList<>();
	final private TaxiRequest initialRequest;
	
	private double earliestPickupTime;
	private double latestPickupTime;
	
	private AVPickupTask pickupTask;
	private AVDropoffTask dropoffTask;
	
	public AVAggregateTaxiRequest(TaxiRequest request) {
		initialRequest = request;
		requests.add(request);
		
		earliestPickupTime = request.getT0() - 10;
		latestPickupTime = request.getT0() + 10;
	}
	
	public boolean covers(TaxiRequest request) {
		return (request.getT0() >= earliestPickupTime && request.getT1() <= latestPickupTime);
	}
	
	public void addRequest(TaxiRequest request) {
		requests.add(request);
		
		if (!covers(request)) {
			throw new IllegalStateException();
		}
		
		if (!(getPickupLink().equals(request.getFromLink()) && getDropoffLink().equals(request.getToLink()))) {
			throw new IllegalStateException();
		}
	}
	
	public double getEarliestPickupTime() {
		return earliestPickupTime;
	}
	
	public double getLatestPickupTime() {
		return latestPickupTime;
	}
	
	public Link getPickupLink() {
		return initialRequest.getFromLink();
	}
	
	public Link getDropoffLink() {
		return initialRequest.getToLink();
	}
	
	public List<TaxiRequest> getRequests() {
		return requests;
	}
	
	public void setTasks(AVPickupTask pickupTask, AVDropoffTask dropoffTask) {
		this.pickupTask = pickupTask;
		this.dropoffTask = dropoffTask;
	}
	
	public AVPickupTask getPickupTask() {
		return pickupTask;
	}
	
	public AVDropoffTask getDropoffTask() {
		return dropoffTask;
	}
}
