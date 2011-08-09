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
				tourAct = new DepotDelivery(customer);
				tourAct.setTimeWindow(start, end);
			}
			else if(customer.getDemand() > 0){
				tourAct = new DepotPickup(customer);
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
