/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
	
	@Override
	public String toString() {
		return "fromNode=" + from.getLocation().getId() + " toNode=" + to.getLocation().getId() + " size=" + from.getDemand();
	}

}
