package lsp.controler;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.vehicles.Vehicle;


class MobSimVehicleRoute {

	private Plan plan;
	
	private Vehicle vehicle;

	public MobSimVehicleRoute(Plan plan, Vehicle vehicle) {
		super();
		this.plan = plan;
		this.vehicle = vehicle;
	}

	/**
	 * @return the plan
	 */
	public Plan getPlan() {
		return plan;
	}

	/**
	 * @return the vehicle
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}
	
	

}
