package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingOccupancyAnalysis;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceAnalysis;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingScoreExecutor;

public class FinishParkingOccupancyMaintainer implements AfterMobsimListener {

	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// close handler processing
		ParkingRoot.getParkingOccupancyMaintainer().closeAllLastParkings();

		// write occupancy statistics
		// TODO: refactor methods out of this!
		ParkingOccupancyAnalysis poaWriter = new ParkingOccupancyAnalysis(ParkingRoot.getParkingOccupancyMaintainer()
				.getParkingOccupancyBins(), ParkingRoot.getParkingCapacity());
		String fileName = event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingOccupancyStatistics.txt");
		poaWriter.writeTxtFile(fileName);
		fileName = event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingOccupancyCoordinates.txt");
		poaWriter.writeFakeKMLFile(fileName);
		
		writeOccupancyViolationStatisticsGrpah(poaWriter);

		fileName = event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingWalkingTimes.txt");
		new ParkingWalkingDistanceAnalysis(ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance())
				.writeTxtFile(fileName);
		fileName = event.getControler().getControlerIO()
		.getIterationFilename(event.getControler().getIterationNumber(), "parkingLog.txt");
		GeneralLib.writeList(ParkingRoot.getParkingLog(),fileName);
		
		writeWalkingDistanceStatisticsGraph();
		generatePersonGroupsWalkingDistanceGraph();
		updateparkingWalkingTimeOfPreviousIteration();
		
		//ParkingRoot.writeMapDebugTraceToCurrentIterationDirectory();
		//ParkingRoot.resetMapDebugTrace();
		
		// perform scoring
		new ParkingScoreExecutor().performScoring(event);
		
	}
	
	private void generatePersonGroupsWalkingDistanceGraph() {
		
		PersonGroups personGroups = ParkingRoot.getPersonGroupsForStatistics();
		
		if (personGroups==null){
			return;
		}
		
		
		HashMap<Id, Double> parkingRelatedWalkDistance = ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance();
		
		for (Id personId:parkingRelatedWalkDistance.keySet()){
			int iterationNumber=GlobalRegistry.controler.getIterationNumber();
			String attribute=PersonGroupWalkingDistanceGraphGenerator.iterationWalkingDistanceSum+iterationNumber;
			
			Double sumOfWalkingDistance = (Double) personGroups.getAttributeValueForGroupToWhichThePersonBelongs(personId, attribute);
			
			if (sumOfWalkingDistance==null){
				sumOfWalkingDistance=0.0;
			}
			
			sumOfWalkingDistance+=parkingRelatedWalkDistance.get(personId)*ParkingRoot.getParkingDistanceScalingFactorForOutput();
			
			personGroups.setAttributeValueForGroupToWhichThePersonBelongs(personId, attribute, sumOfWalkingDistance);
			
		}
		
		PersonGroupWalkingDistanceGraphGenerator.generateGraphic(ParkingRoot.getPersonGroupsForStatistics(), GlobalRegistry.controler.getControlerIO().getOutputFilename("personGroupsWalkingDistance.png"));
	}

	private void writeOccupancyViolationStatisticsGrpah(
			ParkingOccupancyAnalysis poaWriter) {
		
		Controler controler=GlobalRegistry.controler;
		ParkingOccupancyAnalysis.updateStatisticsForIteration(controler.getIterationNumber(), poaWriter);
		String fileName = GlobalRegistry.controler.getControlerIO().getOutputFilename("occupancyViolations.png");
		ParkingOccupancyAnalysis.writeStatisticsGraph(fileName);
		
	}

	private void writeWalkingDistanceStatisticsGraph(){
		Controler controler=GlobalRegistry.controler;
		ParkingRoot.getParkingWalkingDistanceGraph().updateStatisticsForIteration(controler.getIterationNumber(), ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance());
		String fileName = GlobalRegistry.controler.getControlerIO().getOutputFilename("walkingDistance.png");
		ParkingRoot.getParkingWalkingDistanceGraph().writeGraphic(fileName);
	}
	
	private void updateparkingWalkingTimeOfPreviousIteration(){
		ParkingRoot.setParkingWalkingDistanceOfPreviousIteration(ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance());
	}
	
}
