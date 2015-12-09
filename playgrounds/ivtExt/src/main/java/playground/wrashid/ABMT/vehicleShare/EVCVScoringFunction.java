package playground.wrashid.ABMT.vehicleShare;

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
				this.score += evCosts.getInitialInvestmentCost() * GlobalTESFParameters.weightParameterPerDay;
				this.score += DistanceTravelledWithCar.distanceTravelled.get(person.getId()) * evCosts.getPerMeterTravelCost() * GlobalTESFParameters.weightParameterPerMeter;
				
				if (DistanceTravelledWithCar.distanceTravelled.get(person.getId()) > GlobalTESFParameters.evDailyRange){
					this.score += -1000000.0;
				}
						
			} else {
				this.score += cvCosts.getInitialInvestmentCost() * GlobalTESFParameters.weightParameterPerDay;
				this.score += DistanceTravelledWithCar.distanceTravelled.get(person.getId()) * cvCosts.getPerMeterTravelCost() * GlobalTESFParameters.weightParameterPerMeter;		
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


