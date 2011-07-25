package playground.wrashid.parkingChoice.apiDefImpl;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.api.ParkingScoringFunction;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.scoring.ParkingScoreAccumulator;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class ParkingScoringFunctionZhScenario_v1 implements ParkingScoringFunction {

	public static double disutilityOfWalkingPerMeter; // should be negative
	public static double disutilityOfWalkingPowerFactor; // must be positive
	
	public static double streetParkingPricePerSecond; // should be positive
	public static double garageParkingPricePerSecond; // should be positive
	

	public ParkingScoringFunctionZhScenario_v1(){
	}
	
	// TODO: income auch noch hier einfliessen lassen für weitere experimente
	// TODO: time of day pricing auch noch einfliessen lassen.
	@Override
	public void assignScore(ParkingImpl parking, Coord targtLocationCoord, ActInfo targetActInfo, Id personId,
			Double arrivalTime, Double estimatedParkingDuration) {
		double totalScore = getScore(parking, targtLocationCoord,targetActInfo,personId,arrivalTime, estimatedParkingDuration);
		parking.setScore(totalScore);
		ParkingScoreAccumulator.scores.incrementBy(personId, totalScore);
	}

	@Override
	public Double getScore(ParkingImpl parking, Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime,
			Double estimatedParkingDuration) {
		double distanzLindenhof = GeneralLib.getDistance(parking.getCoord(), ParkingHerbieControler.getCoordinatesLindenhofZH());
		
		double walkingDistance = GeneralLib.getDistance(parking.getCoord(), targtLocationCoord);
		double walkingScore=walkingDistance*disutilityOfWalkingPerMeter;
		
		if (walkingDistance>1500.0){
			walkingScore+=walkingDistance*disutilityOfWalkingPerMeter*2;
			System.out.println();
		}
		
		double priceScore=0.0;
		if (parking.getId().toString().startsWith("stp")){
			if (distanzLindenhof<1000){
				priceScore-=estimatedParkingDuration*streetParkingPricePerSecond;
			}
		} else if (parking.getId().toString().startsWith("gp")){
			priceScore-=estimatedParkingDuration*garageParkingPricePerSecond;
		} else {
			
		}
		
		double totalScore=walkingScore+priceScore;
		return totalScore;
	}

}
