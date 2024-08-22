package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;

import jakarta.inject.Provider;
import java.util.List;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftScheduler extends Provider<DrtShiftsSpecification> {

    List<DrtShift> schedule(double time);
    ImmutableMap<Id<DrtShift>, DrtShift> initialSchedule();

}
