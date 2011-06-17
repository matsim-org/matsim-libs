package vrp.basics;

import vrp.api.Customer;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Relation {
	
	private Customer relatedCustomer;
	
	public Relation(Customer relatedCustomer) {
		super();
		this.relatedCustomer = relatedCustomer;
	}

	public Customer getCustomer() {
		return relatedCustomer;
	}
}