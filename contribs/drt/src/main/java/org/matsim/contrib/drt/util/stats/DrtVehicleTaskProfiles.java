package org.matsim.contrib.drt.util.stats;

import com.google.common.collect.ImmutableMap;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.stats.VehicleTaskProfileCalculator;
import org.matsim.contrib.util.stats.VehicleTaskProfileWriter;
import org.matsim.core.controler.MatsimServices;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;

/**
 * Based on {@link DrtVehicleOccupancyProfiles}
 *
 * @author nkuehnel / MOIA
 */
public class DrtVehicleTaskProfiles {

	private static final Comparator<Task.TaskType> taskTypeComparator = Comparator.comparing(type -> {
		//we want the following order on the plot: STAY, RELOCATE, other
		if (type.equals(DrtStayTask.TYPE)) {
			return "B";
		} else if (type.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
			return "A";
		} else {
			return "C" + type.name();
		}
	});

	private static final Map<Task.TaskType, Paint> taskTypePaints = ImmutableMap.of(DrtStayTask.TYPE, Color.LIGHT_GRAY);

	public static VehicleTaskProfileWriter createProfileWriter(MatsimServices matsimServices, String mode,
															   VehicleTaskProfileCalculator calculator) {
		return new VehicleTaskProfileWriter(matsimServices, mode, calculator, taskTypeComparator,
				taskTypePaints);
	}
}

