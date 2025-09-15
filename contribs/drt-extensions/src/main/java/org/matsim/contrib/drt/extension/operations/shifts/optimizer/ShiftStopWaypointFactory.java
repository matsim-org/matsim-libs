package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.optimizer.StopWaypointFactory;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftStopWaypointFactory implements StopWaypointFactory {

    private final StopWaypointFactory delegate;
    private final DvrpLoadType type;

    public ShiftStopWaypointFactory(StopWaypointFactory delegate, DvrpLoadType type) {
        this.delegate = delegate;
        this.type = type;
    }

    @Override
    public StopWaypoint createStopWaypoint(DrtStopTask task, DvrpLoad outgoingOccupancy) {
        return switch (task) {
            case ShiftBreakTask shiftBreakTask -> new ShiftBreakStopWaypoint(shiftBreakTask, type);
            case ShiftChangeOverTask shiftChangeOverTask -> new ShiftChangeoverStopWaypoint(shiftChangeOverTask, type);
            default -> delegate.createStopWaypoint(task, outgoingOccupancy);
        };
    }
}
