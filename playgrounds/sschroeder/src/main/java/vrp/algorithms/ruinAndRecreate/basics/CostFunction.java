package vrp.algorithms.ruinAndRecreate.basics;

/**
 * 
 * @author stefan schroeder
 *
 */

public class CostFunction {
	
	private Double cost = null;

	private double marginalCostOfVehicle = 0;
	
	public void reset() {		
		this.cost = null;
	}

	public void add(double cost) {
		if(this.cost == null){
			this.cost = cost;
		}
		else{
			this.cost += cost;
		}
		this.cost += marginalCostOfVehicle;
	}

	public double getTotalCost() {
		return cost;
	}
}
