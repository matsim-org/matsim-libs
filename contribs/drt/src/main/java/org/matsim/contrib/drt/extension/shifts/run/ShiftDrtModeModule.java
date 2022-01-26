package org.matsim.contrib.drt.extension.shifts.run;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.shifts.analysis.*;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecificationImpl;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftBreakTaskImpl;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeoverTaskImpl;
import org.matsim.contrib.drt.extension.shifts.schedule.WaitForShiftStayTask;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftsSpecificationImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileWriter;
import org.matsim.contrib.util.stats.VehicleTaskProfileCalculator;
import org.matsim.contrib.util.stats.VehicleTaskProfileWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtModeModule extends AbstractDvrpModeModule {

    private final DrtConfigGroup drtConfigGroup;
	private final ShiftDrtConfigGroup shiftConfig;

	public ShiftDrtModeModule(DrtConfigGroup drtCfg, ShiftDrtConfigGroup shiftCfg) {
        super(drtCfg.getMode());
        this.drtConfigGroup = drtCfg;
		this.shiftConfig = shiftCfg;
	}

	private static final Comparator<Task.TaskType> taskTypeComparator = Comparator.comparing((Task.TaskType type) -> {
		//we want the following order on the plot: STAY, RELOCATE, other
		if (type.equals(WaitForShiftStayTask.TYPE)) {
			return "F";
		} else if(type.equals(ShiftChangeoverTaskImpl.TYPE)) {
			return "E";
		} else if(type.equals(ShiftBreakTaskImpl.TYPE)) {
			return "D";
		} else if(DrtTaskBaseType.STAY.isBaseTypeOf(type)) {
			return "C";
		} else if (type.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE) ) {
			return "B";
		} else {
			return "A" + type.name();
		}
	}).reversed();

	private static final Map<Task.TaskType, Paint> taskTypePaints = ImmutableMap.of(
			WaitForShiftStayTask.TYPE, Color.WHITE,
			ShiftChangeoverTaskImpl.TYPE, Color.GRAY,
			ShiftBreakTaskImpl.TYPE, Color.DARK_GRAY,
			DrtStayTask.TYPE, Color.LIGHT_GRAY);


	@Override
    public void install() {
		if (shiftConfig.getShiftInputFile() != null) {
			bindModal(DrtShiftsSpecification.class).toProvider(() -> {
				DrtShiftsSpecification drtShiftsSpecification = new DrtShiftsSpecificationImpl();
				new DrtShiftsReader(drtShiftsSpecification).readURL(shiftConfig.getShiftInputUrl(getConfig().getContext()));
				return drtShiftsSpecification;
			}).asEagerSingleton();
		}

		if (shiftConfig.getOperationFacilityInputFile() != null) {
			bindModal(OperationFacilitiesSpecification.class).toProvider(() -> {
				OperationFacilitiesSpecification operationFacilitiesSpecification = new OperationFacilitiesSpecificationImpl();
				new OperationFacilitiesReader(operationFacilitiesSpecification).readURL(shiftConfig.getOperationFacilityInputUrl(getConfig().getContext()));
				return operationFacilitiesSpecification;
			}).asEagerSingleton();
		}

		bindModal(ShiftDurationXY.class).toProvider(modalProvider(
				getter -> new ShiftDurationXY(getter.getModal(DrtShiftsSpecification.class), getter.get(EventsManager.class)))).asEagerSingleton();

		bindModal(BreakCorridorXY.class).toProvider(modalProvider(
				getter -> new BreakCorridorXY(getter.getModal(DrtShiftsSpecification.class), getter.get(EventsManager.class)))).asEagerSingleton();

		bindModal(ShiftHistogram.class).toProvider(modalProvider(
				getter -> new ShiftHistogram(getter.get(Population.class), getter.get(EventsManager.class)))).asEagerSingleton();

		addEventHandlerBinding().to(modalKey(ShiftDurationXY.class));
		addEventHandlerBinding().to(modalKey(BreakCorridorXY.class));
		addEventHandlerBinding().to(modalKey(ShiftHistogram.class));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new ShiftHistogramListener(drtConfigGroup,
						getter.get(MatsimServices.class), getter.getModal(ShiftHistogram.class)))).asEagerSingleton();
		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new ShiftAnalysisControlerListener(drtConfigGroup,
						getter.getModal(ShiftDurationXY.class), getter.getModal(BreakCorridorXY.class),
						getter.get(MatsimServices.class)))).asEagerSingleton();

		bindModal(DumpShiftDataAtEndImpl.class).toProvider(modalProvider(
				getter -> new DumpShiftDataAtEndImpl(
						getter.getModal(DrtShiftsSpecification.class),
						getter.getModal(OperationFacilitiesSpecification.class),
						getter.get(OutputDirectoryHierarchy.class)
				))
		).asEagerSingleton();

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> getter.getModal(DumpShiftDataAtEndImpl.class)
		));

		bindModal(VehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
				getter -> new VehicleOccupancyProfileCalculator(getMode(), getter.getModal(FleetSpecification.class),
						300, getter.get(QSimConfigGroup.class), ImmutableSet.of(DrtDriveTask.TYPE,
						DefaultDrtStopTask.TYPE, ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE,
						ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE)))).asEagerSingleton();

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new VehicleOccupancyProfileWriter(getter.get(MatsimServices.class), drtConfigGroup.getMode(),
						getter.getModal(VehicleOccupancyProfileCalculator.class), taskTypeComparator,
						taskTypePaints, "shift_occupancy_time_profiles"))).in(Singleton.class);

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new VehicleTaskProfileWriter(getter.get(MatsimServices.class), drtConfigGroup.getMode(),
						getter.getModal(VehicleTaskProfileCalculator.class), taskTypeComparator,
						taskTypePaints, "shift_task_time_profiles"))).in(Singleton.class);

		this.installQSimModule(new ShiftDrtQSimModule(getMode()));
    }
}
