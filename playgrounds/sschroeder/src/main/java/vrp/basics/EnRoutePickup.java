package vrp.basics;

import vrp.api.Customer;

/**
 * 
 * @author stefan schroeder
 *
 */

public class EnRoutePickup extends TourActivity{

	public EnRoutePickup(Customer customer) {
		super(customer);
	}

	public String getType(){
		return "EnRoutePickup";
	}
	
}
