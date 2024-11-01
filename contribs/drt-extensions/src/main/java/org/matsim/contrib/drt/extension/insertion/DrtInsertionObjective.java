package org.matsim.contrib.drt.extension.insertion;

import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

public interface DrtInsertionObjective {
	double calculateObjective(DrtRequest request, Insertion insertion, DetourTimeInfo detourTimeInfo);
}
