package herbie;

import org.matsim.api.core.v01.population.Plan;

public class CreateFreightTraffic {

	public static void main(String[] args) {
		CreateFreightTraffic creator = new CreateFreightTraffic();
		creator.create(args[0], args[1]);
	}
	
	public void create(String lkwMatricesPath, String liMatricesPath) {
		this.createPersonsFromODMatrices(lkwMatricesPath);
		this.createPersonsFromODMatrices(liMatricesPath);
	}
	
	private void createPersonsFromODMatrices(String matrixPath) {
		
	}
	
	private void createSingleFreightPlan() {
		
	}
	
	private void assignFacility2Plan(Plan plan) {
		// find random facility within range
		// adapt coordinates
	}
	
	private void addFreightActivity2Facilities() {
		
	}
}
