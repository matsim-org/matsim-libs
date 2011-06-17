package vrp.algorithms.ruinAndRecreate.basics;

import vrp.api.Customer;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Shipment {
	
	private Customer from;
	
	private Customer to;

	public Shipment(Customer from, Customer to) {
		super();
		this.from = from;
		this.to = to;
	}

	public Customer getFrom() {
		return from;
	}

	public Customer getTo() {
		return to;
	}

}
