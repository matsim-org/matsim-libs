package org.matsim.contrib.drt.extension.insertion;

import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

public interface DrtInsertionConstraint {
	boolean checkInsertion(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo);
}
