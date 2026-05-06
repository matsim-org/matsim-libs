package org.matsim.contrib.carsharing.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.SumScoringFunction;


public class CarsharingLegScoringFunction implements SumScoringFunction.BasicScoring {


	private final Config config;
	private final CostsCalculatorContainer costsCalculatorContainer;
	private final DemandHandler demandHandler;
	private final Person person;
	private final CarsharingSupplyInterface carsharingSupplyContainer;

	private double score = 0;

	public CarsharingLegScoringFunction(Config config, DemandHandler demandHandler,
										CostsCalculatorContainer costsCalculatorContainer, CarsharingSupplyInterface carsharingSupplyContainer,
										Person person) {
		this.config = config;
		this.demandHandler = demandHandler;
		this.carsharingSupplyContainer = carsharingSupplyContainer;
		this.costsCalculatorContainer = costsCalculatorContainer;
		this.person = person;
	}

	@Override
	public void finish() {

		AgentRentals agentRentals = this.demandHandler.getAgentRentalsMap().get(person.getId());
		if (agentRentals != null) {
			double marginalUtilityOfMoney = this.config.scoring().getMarginalUtilityOfMoney();
			for (RentalInfo rentalInfo : agentRentals.getArr()) {
				CSVehicle vehicle = this.carsharingSupplyContainer.getAllVehicles().get(rentalInfo.getVehId().toString());
				if (marginalUtilityOfMoney != 0.0)
					score += -1 * this.costsCalculatorContainer.getCost(vehicle.getCompanyId(),
						rentalInfo.getCarsharingType(), rentalInfo) * marginalUtilityOfMoney;
			}
		}
	}

	@Override
	public double getScore() {
		return score;
	}
}
