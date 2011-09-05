package vrp.basics;

import vrp.api.Customer;

/**
 * 
 * @author stefan schroeder
 *
 */

public class EnRouteDelivery extends TourActivity{

	public EnRouteDelivery(Customer customer) {
		super(customer);
	}

	public String getType(){
		return "EnRouteDelivery";
	}
}
