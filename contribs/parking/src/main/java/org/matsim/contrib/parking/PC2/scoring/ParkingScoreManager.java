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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonImpl;

import java.util.Map;

public class ParkingScoreManager {

	private AbstractParkingBetas parkingBetas;
	private double parkingScoreScalingFactor;
	private double randomErrorTermScalingFactor;
	// TODO: also add implementation of random error term + scaling here
	DoubleValueHashMap<Id> scores;
	Controler controler;
	private WalkTravelTime walkTravelTime;
	private RandomErrorTermManager randomErrorTermManager;

	public ParkingScoreManager(WalkTravelTime walkTravelTime, Controler controler) {
		this.walkTravelTime = walkTravelTime;
		this.controler = controler;
	}

	public double calcWalkScore(Coord destCoord, PC2Parking parking, Id personId, double parkingDurationInSeconds) {
        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
		PersonImpl person = (PersonImpl) persons.get(personId);

		double parkingWalkBeta = getParkingBetas().getParkingWalkBeta(person, parkingDurationInSeconds);

        Link link = NetworkUtils.getNearestLink(((NetworkImpl) controler.getScenario().getNetwork()), destCoord);
		double length = link.getLength();
		double walkTime = walkTravelTime.getLinkTravelTime(link, 0, person, null);
		double walkSpeed = length / walkTime;

		// protected double walkSpeed = 3.0 / 3.6; // [m/s]

		double walkDistance = GeneralLib.getDistance(destCoord, parking.getCoordinate());
		double walkDurationInSeconds = walkDistance / walkSpeed;

		double walkingTimeTotalInMinutes = walkDurationInSeconds / 60;

		if (parking.getId().toString().contains("stp")){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		return (parkingWalkBeta * walkingTimeTotalInMinutes) * parkingScoreScalingFactor;
	}

	public double calcCostScore(double arrivalTime, double parkingDurationInSeconds, PC2Parking parking, Id personId) {
        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
		PersonImpl person = (PersonImpl) persons.get(personId);
		double parkingCostBeta = getParkingBetas().getParkingCostBeta(person);

		double parkingCost = parking.getCost(personId, arrivalTime, parkingDurationInSeconds);

		return (parkingCostBeta * parkingCost) * parkingScoreScalingFactor;
	}

	public double calcScore(Coord destCoord, double arrivalTime, double parkingDurationInSeconds, PC2Parking parking, Id personId, int legIndex) {
		double walkScore = calcWalkScore(destCoord, parking, personId, parkingDurationInSeconds);
		double costScore = calcCostScore(arrivalTime, parkingDurationInSeconds, parking, personId);
		double randomError=0;
		
		if (randomErrorTermManager!=null){
			randomError= randomErrorTermManager.getEpsilonAlternative(parking.getId(),personId,legIndex)*randomErrorTermScalingFactor*parkingScoreScalingFactor;
		}
		return costScore + walkScore + randomError;
	}

	public double getScore(Id id) {
		return scores.get(id);
	}

	public synchronized void addScore(Id id, double incValue) {
		scores.incrementBy(id, incValue);
	}

	public synchronized void prepareForNewIteration() {
		scores = new DoubleValueHashMap<Id>();
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
