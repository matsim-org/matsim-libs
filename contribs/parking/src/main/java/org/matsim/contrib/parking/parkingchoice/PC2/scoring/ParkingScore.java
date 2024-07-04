package org.matsim.contrib.parking.parkingchoice.PC2.scoring;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;

public interface ParkingScore {

	double calcWalkScore(Coord destCoord, PC2Parking parking, Id<Person> personId, double parkingDurationInSeconds);

	double calcCostScore(double arrivalTime, double parkingDurationInSeconds, PC2Parking parking, Id<Person> personId);

	double calcScore(Coord destCoord, double arrivalTime, double parkingDurationInSeconds, PC2Parking parking,
			Id<Person> personId, int legIndex, boolean setCostToZero);

	double getScore(Id<Person> id);

	void addScore(Id<Person> id, double incValue);

	void notifyBeforeMobsim();

	double getParkingScoreScalingFactor();

	void setParkingScoreScalingFactor(double parkingScoreScalingFactor);

	double getRandomErrorTermScalingFactor();

	void setRandomErrorTermScalingFactor(double randomErrorTermScalingFactor);

	AbstractParkingBetas getParkingBetas();

	void setParkingBetas(AbstractParkingBetas parkingBetas);

	void setRandomErrorTermManger(RandomErrorTermManager randomErrorTermManager);

}
