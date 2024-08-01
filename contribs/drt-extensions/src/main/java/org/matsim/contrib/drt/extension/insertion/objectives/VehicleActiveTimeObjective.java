package org.matsim.contrib.drt.extension.insertion.objectives;

import org.matsim.contrib.drt.extension.insertion.DrtInsertionObjective;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

public class VehicleActiveTimeObjective implements DrtInsertionObjective {
	@Override
	public double calculateObjective(DrtRequest request, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		return detourTimeInfo.getTotalTimeLoss();
	}
}
