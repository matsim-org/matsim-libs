package org.matsim.contrib.drt.util.stats;

import java.awt.Color;
import java.awt.Paint;
import java.util.Comparator;
import java.util.Map;

import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.stats.ProfileWriter;
import org.matsim.contrib.util.stats.VehicleTaskProfileCalculator;
import org.matsim.contrib.util.stats.VehicleTaskProfileView;
import org.matsim.core.controler.MatsimServices;

import com.google.common.collect.ImmutableMap;

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

	public static ProfileWriter createProfileWriter(MatsimServices matsimServices, String mode, VehicleTaskProfileCalculator calculator) {
		return new ProfileWriter(matsimServices, mode, new VehicleTaskProfileView(calculator, taskTypeComparator, taskTypePaints),
				"task_time_profiles");
	}
}

