/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.parking.PC2.scoring;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public final class ParkingScoreManager {
	
	private Double beelineDistanceFactor = 1.3 ;

	private AbstractParkingBetas parkingBetas;
	private double parkingScoreScalingFactor;
	private double randomErrorTermScalingFactor;
	private DoubleValueHashMap<Id<Person>> scores;
	private final Scenario scenario;
	private final WalkTravelTime walkTravelTime;
	private RandomErrorTermManager randomErrorTermManager;

	public ParkingScoreManager(WalkTravelTime walkTravelTime, Scenario scenario) {
		this.walkTravelTime = walkTravelTime;
		this.scenario = scenario;
	}

	public double calcWalkScore(Coord destCoord, PC2Parking parking, Id<Person> personId, double parkingDurationInSeconds) {
		Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
		Person person = persons.get(personId);

		double parkingWalkBeta = getParkingBetas().getParkingWalkBeta(person, parkingDurationInSeconds);

		Link link = NetworkUtils.getNearestLink((scenario.getNetwork()), destCoord);
		
		double linkLength = link.getLength();		
		
		double walkDistance = CoordUtils.calcEuclideanDistance(destCoord, parking.getCoordinate()) 
				* scenario.getConfig().plansCalcRoute().getBeelineDistanceFactors().get("walk")* beelineDistanceFactor;
		
		double walkSpeed = linkLength / this.walkTravelTime.getLinkTravelTime(link, 0, person, null);

		double walkDurationInSeconds = walkDistance / walkSpeed;

		double walkingTimeTotalInMinutes = walkDurationInSeconds / 60;

		if (parking.getId().toString().contains("stp")){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		return (parkingWalkBeta * walkingTimeTotalInMinutes) * parkingScoreScalingFactor;
	}

	public double calcCostScore(double arrivalTime, double parkingDurationInSeconds, PC2Parking parking, Id<Person> personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		double parkingCostBeta = getParkingBetas().getParkingCostBeta(person);
		double parkingCost = parking.getCost(personId, arrivalTime, parkingDurationInSeconds);
		return (parkingCostBeta * parkingCost) * parkingScoreScalingFactor;
	}
	
	
	public double calcScore(Coord destCoord, double arrivalTime, double parkingDurationInSeconds, PC2Parking parking, Id<Person> personId, int legIndex, boolean setCostToZero) {
		double walkScore = calcWalkScore(destCoord, parking, personId, parkingDurationInSeconds);
		double costScore = calcCostScore(arrivalTime, parkingDurationInSeconds, parking, personId);
		
		if (setCostToZero){
			costScore=0;
		}
		
		double randomError=0;

		if (randomErrorTermManager!=null){
			randomError= randomErrorTermManager.getEpsilonAlternative(parking.getId(),personId,legIndex)*randomErrorTermScalingFactor*parkingScoreScalingFactor;
		}
		return costScore + walkScore + randomError;
	}

	public double getScore(Id<Person> id) {
		return scores.get(id);
	}

	public synchronized void addScore(Id<Person> id, double incValue) {
		scores.incrementBy(id, incValue);
	}

	public synchronized void prepareForNewIteration() {
		scores = new DoubleValueHashMap<>();
	}

	public double getParkingScoreScalingFactor() {
		return parkingScoreScalingFactor;
	}

	public void setParkingScoreScalingFactor(double parkingScoreScalingFactor) {
		this.parkingScoreScalingFactor = parkingScoreScalingFactor;
	}

	public double getRandomErrorTermScalingFactor() {
		return randomErrorTermScalingFactor;
	}

	public void setRandomErrorTermScalingFactor(double randomErrorTermScalingFactor) {
		this.randomErrorTermScalingFactor = randomErrorTermScalingFactor;
	}

	public AbstractParkingBetas getParkingBetas() {
		return parkingBetas;
	}

	public void setParkingBetas(AbstractParkingBetas parkingBetas) {
		this.parkingBetas = parkingBetas;
	}

	public void setRandomErrorTermManger(RandomErrorTermManager randomErrorTermManager) {
		this.randomErrorTermManager = randomErrorTermManager;
	}

}
