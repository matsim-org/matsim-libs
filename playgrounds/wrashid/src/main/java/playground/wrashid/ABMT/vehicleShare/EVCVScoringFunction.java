package playground.wrashid.ABMT.vehicleShare;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

public class EVCVScoringFunction implements BasicScoring {

	private double score = 0;
	private Person person;

	public EVCVScoringFunction(Person person){
		this.person = person;
	}
	
	@Override
	public void finish() {
		VehicleInitializer.initialize(person.getSelectedPlan());
		
		if (VehicleInitializer.hasElectricVehicle.get(person.getSelectedPlan())){
			this.score= -1000;
		} else {
			this.score= 1000;
		}
	}

	@Override
	public double getScore() {
		return this.score;
	}
	
	

}
