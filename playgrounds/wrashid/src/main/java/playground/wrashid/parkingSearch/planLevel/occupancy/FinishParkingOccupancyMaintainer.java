/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingOccupancyAnalysis;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceAnalysis;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingScoreExecutor;

public class FinishParkingOccupancyMaintainer implements AfterMobsimListener {

	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// close handler processing
		ParkingRoot.getParkingOccupancyMaintainer().closeAllLastParkings();

		// write occupancy statistics
		// TODO: refactor methods out of this!
		ParkingOccupancyAnalysis poaWriter = new ParkingOccupancyAnalysis(ParkingRoot.getParkingOccupancyMaintainer()
				.getParkingOccupancyBins(), ParkingRoot.getParkingCapacity());
		String fileName = event.getServices().getControlerIO()
				.getIterationFilename(event.getIteration(), "parkingOccupancyStatistics.txt");
		poaWriter.writeTxtFile(fileName);
		fileName = event.getServices().getControlerIO()
				.getIterationFilename(event.getIteration(), "parkingOccupancyCoordinates.txt");
		poaWriter.writeFakeKMLFile(fileName);
		
		writeOccupancyViolationStatisticsGraph(event.getIteration(), poaWriter);

		fileName = event.getServices().getControlerIO()
				.getIterationFilename(event.getIteration(), "parkingWalkingTimes.txt");
		new ParkingWalkingDistanceAnalysis(ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance())
				.writeTxtFile(fileName);
		fileName = event.getServices().getControlerIO()
		.getIterationFilename(event.getIteration(), "parkingLog.txt");
		GeneralLib.writeList(ParkingRoot.getParkingLog(),fileName);
		
		writeWalkingDistanceStatisticsGraph(event.getIteration());
		generatePersonGroupsWalkingDistanceGraph(event.getIteration());
		updateparkingWalkingTimeOfPreviousIteration();
		
		//ParkingRoot.writeMapDebugTraceToCurrentIterationDirectory();
		//ParkingRoot.resetMapDebugTrace();
		
		// perform scoring
		new ParkingScoreExecutor().performScoring(event);
		
	}
	
	private void generatePersonGroupsWalkingDistanceGraph(int iteration) {
		
		PersonGroups personGroups = ParkingRoot.getPersonGroupsForStatistics();
		
		if (personGroups==null){
			return;
		}
		
		
		HashMap<Id<Person>, Double> parkingRelatedWalkDistance = ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance();
		
		for (Id<Person> personId:parkingRelatedWalkDistance.keySet()){
			int iterationNumber=iteration;
			String attribute=PersonGroupWalkingDistanceGraphGenerator.iterationWalkingDistanceSum+iterationNumber;
			
			Double sumOfWalkingDistance = (Double) personGroups.getAttributeValueForGroupToWhichThePersonBelongs(personId, attribute);
			
			if (sumOfWalkingDistance==null){
				sumOfWalkingDistance=0.0;
			}
			
			sumOfWalkingDistance+=parkingRelatedWalkDistance.get(personId)*ParkingRoot.getParkingWalkingDistanceScalingFactorForOutput();
			
			personGroups.setAttributeValueForGroupToWhichThePersonBelongs(personId, attribute, sumOfWalkingDistance);
			
		}
		
		PersonGroupWalkingDistanceGraphGenerator.generateGraphic(ParkingRoot.getPersonGroupsForStatistics(), GlobalRegistry.controler.getControlerIO().getOutputFilename("personGroupsWalkingDistance.png"));
	}

	private void writeOccupancyViolationStatisticsGraph(int iteration, 
			ParkingOccupancyAnalysis poaWriter) {
		
		ParkingOccupancyAnalysis.updateStatisticsForIteration(iteration, poaWriter);
		String fileName = GlobalRegistry.controler.getControlerIO().getOutputFilename("occupancyViolations.png");
		ParkingOccupancyAnalysis.writeStatisticsGraph(fileName);
		
	}

	private void writeWalkingDistanceStatisticsGraph(int iteration){
		ParkingRoot.getParkingWalkingDistanceGraph().updateStatisticsForIteration(iteration, ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance());
		String fileName = GlobalRegistry.controler.getControlerIO().getOutputFilename("walkingDistance.png");
		ParkingRoot.getParkingWalkingDistanceGraph().writeGraphic(fileName);
	}
	
	private void updateparkingWalkingTimeOfPreviousIteration(){
		ParkingRoot.setParkingWalkingDistanceOfPreviousIteration(ParkingRoot.getParkingOccupancyMaintainer().getParkingRelatedWalkDistance());
	}
	
}
