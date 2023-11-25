package org.matsim.contrib.drt.extension.operations.shifts.shift;

import java.util.Optional;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShift extends Identifiable<DrtShift> {

  double getStartTime();

  double getEndTime();

  Optional<DrtShiftBreak> getBreak();

  boolean isStarted();

  boolean isEnded();

  void start();

  void end();

  Optional<Id<OperationFacility>> getOperationFacilityId();
}
