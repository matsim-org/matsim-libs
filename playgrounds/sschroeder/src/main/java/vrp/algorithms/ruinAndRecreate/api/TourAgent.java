package vrp.algorithms.ruinAndRecreate.api;

import java.util.Collection;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.api.Constraints;
import vrp.api.Customer;
import vrp.basics.Tour;
import vrp.basics.TourActivity;


/**
 * 
 * @author stefan schroeder
 *
 */

public interface TourAgent extends ServiceProvider, Runnable{

	abstract int getTourSize();

	public abstract double getTotalCost();

	public abstract void removeCustomer(Customer customer);

	abstract boolean hasCustomer(Customer n);

	abstract int getVehicleCapacity();

	abstract Collection<TourActivity> getTourActivities();
	
	abstract Tour getTour();
	
	abstract void setConstraint(Constraints constraint);
	
	public abstract boolean tourIsValid();
	
	public Offer getOpenOffer();
	
	public void setNewShipment(Shipment shipment);

}