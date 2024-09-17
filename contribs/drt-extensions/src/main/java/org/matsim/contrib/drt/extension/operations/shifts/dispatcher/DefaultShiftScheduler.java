package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.shift.*;
import org.matsim.contrib.dvrp.fleet.Fleet;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author nkuehnel / MOIA
 */
public final class DefaultShiftScheduler implements ShiftScheduler {

    private final DrtShiftsSpecification shiftsSpecification;
    public static Function<DrtShiftSpecification, DrtShift> createShiftFromSpec = spec -> {
        DefaultShiftBreakImpl shiftBreak = null;
        DrtShiftBreakSpecification breakSpec = spec.getBreak().orElse(null);
        if (breakSpec != null) {
            shiftBreak = new DefaultShiftBreakImpl(
                    breakSpec.getEarliestBreakStartTime(),
                    breakSpec.getLatestBreakEndTime(),
                    breakSpec.getDuration());
        }
        return (DrtShift) new DrtShiftImpl(spec.getId(), spec.getStartTime(), spec.getEndTime(),
                spec.getOperationFacilityId().orElse(null), spec.getDesignatedVehicleId().orElse(null),
                shiftBreak);
    };

    public DefaultShiftScheduler(DrtShiftsSpecification shiftsSpecification) {
        this.shiftsSpecification = shiftsSpecification;
    }
    @Override
    public List<DrtShift> schedule(double time, Fleet fleet) {
        return Collections.emptyList();
    }

    @Override
    public ImmutableMap<Id<DrtShift>, DrtShift> initialSchedule() {
        return shiftsSpecification.getShiftSpecifications().values()
                .stream()
                .map(createShiftFromSpec)
                .collect(ImmutableMap.toImmutableMap(DrtShift::getId, s -> s));
    }

    @Override
    public DrtShiftsSpecification get() {
        return shiftsSpecification;
    }
}
