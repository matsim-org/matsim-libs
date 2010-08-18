package playground.wrashid.parkingSearch.planLevel.init;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingCapacity;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyMaintainer;
import playground.wrashid.parkingSearch.planLevel.parkingActivityDuration.ParkingActivityDuration;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping1;
import playground.wrashid.parkingSearch.planLevel.ranking.ClosestParkingMatrix;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingScoringFunction;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingDefaultScoringFunction;

public class ParkingRoot {

	private static ClosestParkingMatrix cpm = null;
	private static LinkParkingFacilityAssociation lpfa = null;
	private static ParkingCapacity pc = null;
	private static double parkingPriceScoreScalingFactor;
	private static double parkingActivityDurationPenaltyScalingFactor;
	private static ParkingOccupancyMaintainer parkingOccupancyMaintainer;
	private static ParkingActivityDuration parkingActivityDuration;
	private static ParkingScoringFunction parkingScoringFunction;
	private static ArrayList<String> parkingLog;
	private static BasicPointVisualizer mapDebugTrace;
	private static ParkingWalkingDistanceMeanAndStandardDeviationGraph parkingWalkingDistanceGraph;
	private static HashMap<Id, Double> parkingWalkingDistanceOfPreviousIteration=null;
	private static PersonGroups personGroupsForStatistics=null;


	public static ParkingWalkingDistanceMeanAndStandardDeviationGraph getParkingWalkingDistanceGraph() {
		return parkingWalkingDistanceGraph;
	}

	public static BasicPointVisualizer getMapDebugTrace() {
		return mapDebugTrace;
	}
	
	public static void resetMapDebugTrace(){
		mapDebugTrace=new BasicPointVisualizer();
	}
	
	public static void writeMapDebugTraceToCurrentIterationDirectory(){
		String fileName = GlobalRegistry.controler.getControlerIO().getOutputFilename("mapDebugTrace.kml");
		ParkingRoot.getMapDebugTrace().write(fileName);
	}

	public static void setParkingScoringFunction(ParkingScoringFunction parkingScoringFunction) {
		ParkingRoot.parkingScoringFunction = parkingScoringFunction;
	}

	public static ParkingScoringFunction getParkingScoringFunction() {
		return parkingScoringFunction;
	}

	public static ParkingActivityDuration getParkingActivityDuration() {
		return parkingActivityDuration;
	}

	public static double getPriceScoreScalingFactor() {
		return parkingPriceScoreScalingFactor;
	}

	public static double getParkingActivityDurationPenaltyScalingFactor() {
		return parkingActivityDurationPenaltyScalingFactor;
	}

	public static void init(ActivityFacilitiesImpl facilities, NetworkLayer network, Controler controler) {
		parkingWalkingDistanceGraph=new ParkingWalkingDistanceMeanAndStandardDeviationGraph();
		mapDebugTrace=new BasicPointVisualizer();
		cpm = new ClosestParkingMatrix(facilities, network);
		lpfa = new LinkParkingFacilityAssociation(facilities, network);
		pc = new ParkingCapacity(facilities);
		parkingActivityDuration = new ParkingActivityDuration();
		parkingLog=new ArrayList<String>();

		String tempStringValue = controler.getConfig().findParam("parking", "parkingPriceScoreScalingFactor");
		checkIfNull(tempStringValue);
		parkingPriceScoreScalingFactor = Double.parseDouble(tempStringValue);

		tempStringValue = controler.getConfig().findParam("parking", "parkingActivityDurationPenaltyScalingFactor");
		checkIfNull(tempStringValue);
		parkingActivityDurationPenaltyScalingFactor = Double.parseDouble(tempStringValue);

		// set default scoring function, if no scoring function set
		if (parkingScoringFunction == null) {
			parkingScoringFunction = new ParkingDefaultScoringFunction(new ParkingPriceMapping1(),
					new IncomeRelevantForParking(), facilities);
		} else {
			// in order to allow setting the scoring function from outside
			// we must set the facilities at this stage
			// they can be set null, at beginning when facilities are not yet loaded.
			parkingScoringFunction.setParkingFacilities(facilities);
		}
		
		

	}

	public static void setRanking(ParkingScoringFunction ranking) {
		ParkingRoot.parkingScoringFunction = ranking;
	}

	public static ClosestParkingMatrix getClosestParkingMatrix() {
		checkIfNull(cpm);
		return cpm;
	}

	public static LinkParkingFacilityAssociation getLinkParkingFacilityAssociation() {
		checkIfNull(lpfa);
		return lpfa;
	}

	public static ParkingCapacity getParkingCapacity() {
		checkIfNull(pc);
		return pc;
	}

	private static void checkIfNull(Object obj) {
		if (obj == null) {
			throw new Error("Please initialize the variables first.");
		}

	}

	public static void setParkingOccupancyMaintainer(ParkingOccupancyMaintainer _parkingOccupancyMaintainer) {
		parkingOccupancyMaintainer = _parkingOccupancyMaintainer;
	}

	public static ParkingOccupancyMaintainer getParkingOccupancyMaintainer() {
		return parkingOccupancyMaintainer;
	}

	public static ArrayList<String> getParkingLog() {
		return parkingLog;
	}

	public static void setParkingWalkingDistanceOfPreviousIteration(
			HashMap<Id, Double> parkingWalkingDistanceOfPreviousIteration) {
		ParkingRoot.parkingWalkingDistanceOfPreviousIteration = parkingWalkingDistanceOfPreviousIteration;
	}

	public static HashMap<Id, Double> getParkingWalkingDistanceOfPreviousIteration() {
		return parkingWalkingDistanceOfPreviousIteration;
	}

	public static void setPersonGroupsForStatistics(
			PersonGroups personGroupsForStatistics) {
		ParkingRoot.personGroupsForStatistics = personGroupsForStatistics;
	}

	public static PersonGroups getPersonGroupsForStatistics() {
		return personGroupsForStatistics;
	}

}
