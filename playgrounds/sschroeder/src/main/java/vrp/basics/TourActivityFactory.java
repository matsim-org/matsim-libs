/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
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
package vrp.basics;

import vrp.api.Customer;

public class TourActivityFactory {
	
	public TourActivity createTourActivity(Customer customer){
		TourActivity tourAct = null;
		double start = customer.getTheoreticalTimeWindow().getStart();
		double end = customer.getTheoreticalTimeWindow().getEnd();
		if(customer.hasRelation()){
			if(customer.getDemand() < 0){
				tourAct = new EnRouteDelivery(customer);
				tourAct.setTimeWindow(start, end);
			}
			else if(customer.getDemand() > 0){
				tourAct = new EnRoutePickup(customer);
				tourAct.setTimeWindow(start, end);
			}
			else {
				tourAct = new OtherDepotActivity(customer);
				tourAct.setTimeWindow(start, end);
			}
		}
		else{
			if(customer.getDemand() < 0){
				tourAct = new DeliveryFromDepot(customer);
				tourAct.setTimeWindow(start, end);
			}
			else if(customer.getDemand() > 0){
				tourAct = new PickupToDepot(customer);
				tourAct.setTimeWindow(start, end);
			}
			else {
				tourAct = new OtherDepotActivity(customer);
				tourAct.setTimeWindow(start, end);
			}
		}
		return tourAct;
	}

}
