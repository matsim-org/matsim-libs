package playground.wrashid.ABMT.vehicleShare;

import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * 
 * @author wrashid
 *
 */
public class EVCVScoringFunction implements BasicScoring {

	private double score = 0;
	private Person person;

	public static CVCosts cvCosts = new CVCosts();
	public static EVCosts evCosts = new EVCosts();

	public EVCVScoringFunction(Person person) {
		this.person = person;
	}

	@Override
	public void finish() {
		Plan selectedPlan = person.getSelectedPlan();
		VehicleInitializer.initialize(selectedPlan);
		if (VehicleInitializer.hasCarLeg(selectedPlan)) {

			if (VehicleInitializer.hasElectricVehicle.get(selectedPlan)) {
				this.score += evCosts.getInitialInvestmentCost();
				this.score += DistanceTravelledWithCar.distanceTravelled.get(person.getId()) * evCosts.getPerMeterTravelCost();
		
			} else {
				this.score += cvCosts.getInitialInvestmentCost();
				this.score += DistanceTravelledWithCar.distanceTravelled.get(person.getId()) * cvCosts.getPerMeterTravelCost();
			}
			
			if (TollsManager.tollDisutilities.containsKey(person.getId())){
				this.score+=TollsManager.tollDisutilities.get(person.getId());
			}
			
		}
	}

	@Override
	public double getScore() {
		return this.score;
	}

}


