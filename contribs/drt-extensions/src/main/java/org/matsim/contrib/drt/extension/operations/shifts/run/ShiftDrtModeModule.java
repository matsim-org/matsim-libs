package org.matsim.contrib.drt.extension.operations.shifts.run;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.analysis.*;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.ShiftScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultShiftScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTaskImpl;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeoverTaskImpl;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecificationImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.analysis.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.dvrp.analysis.VehicleOccupancyProfileView;
import org.matsim.contrib.dvrp.analysis.VehicleTaskProfileCalculator;
import org.matsim.contrib.dvrp.analysis.VehicleTaskProfileView;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.config.Config;
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
	private final DrtOperationsParams drtOperationsParams;

	public ShiftDrtModeModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtConfigGroup = drtCfg;
		this.drtOperationsParams = ((DrtWithExtensionsConfigGroup) drtCfg).getDrtOperationsParams().orElseThrow();
	}

	private static final Comparator<Task.TaskType> taskTypeComparator = Comparator.comparing((Task.TaskType type) -> {
		//we want the following order on the plot: STAY, RELOCATE, other
		if (type.equals(WaitForShiftTask.TYPE)) {
			return "F";
		} else if (type.equals(ShiftChangeoverTaskImpl.TYPE)) {
			return "E";
		} else if (type.equals(ShiftBreakTaskImpl.TYPE)) {
			return "D";
		} else if (DrtTaskBaseType.STAY.isBaseTypeOf(type)) {
			return "C";
		} else if (type.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
			return "B";
		} else {
			return "A" + type.name();
		}
	}).reversed();

	private static final Map<Task.TaskType, Paint> taskTypePaints = ImmutableMap.of(
			WaitForShiftTask.TYPE, Color.WHITE,
			ShiftChangeoverTaskImpl.TYPE, Color.GRAY,
			ShiftBreakTaskImpl.TYPE, Color.DARK_GRAY,
			DrtStayTask.TYPE, Color.LIGHT_GRAY);


	@Override
	public void install() {
		ShiftsParams shiftsParams = drtOperationsParams.getShiftsParams().orElseThrow();

		DrtShiftsSpecification drtShiftsSpecification = new DrtShiftsSpecificationImpl();
		if (shiftsParams.shiftInputFile != null) {
			new DrtShiftsReader(drtShiftsSpecification).readURL(shiftsParams.getShiftInputUrl(getConfig().getContext()));
		}

		bindModal(ShiftScheduler.class).toProvider(modalProvider(getter -> new DefaultShiftScheduler(drtShiftsSpecification)));
		bindModal(DrtShiftsSpecification.class).toProvider(modalKey(ShiftScheduler.class));

		bindModal(ShiftDurationXY.class).toProvider(modalProvider(
				getter -> new ShiftDurationXY(getter.getModal(new TypeLiteral<Provider<DrtShiftsSpecification>>(){}), getMode())
		)).asEagerSingleton();

		bindModal(BreakCorridorXY.class).toProvider(modalProvider(
				getter -> new BreakCorridorXY(getMode(), getter.getModal(new TypeLiteral<Provider<DrtShiftsSpecification>>(){}))
		)).asEagerSingleton();

		bindModal(ShiftHistogram.class).toProvider(modalProvider(
				getter -> new ShiftHistogram(getMode(), getter.get(Config.class)))).asEagerSingleton();

		addEventHandlerBinding().to(modalKey(ShiftDurationXY.class));
		addEventHandlerBinding().to(modalKey(BreakCorridorXY.class));
		addEventHandlerBinding().to(modalKey(ShiftHistogram.class));

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new ShiftHistogramListener(drtConfigGroup,
						getter.get(MatsimServices.class), getter.getModal(ShiftHistogram.class)))).asEagerSingleton();
		addControlerListenerBinding().toProvider(modalProvider(
				getter -> new ShiftAnalysisControlerListener(getConfig(), drtConfigGroup,
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
						ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE), getter.getModal(DvrpLoadType.class)))).asEagerSingleton();

		addControlerListenerBinding().toProvider(modalProvider(getter -> new ProfileWriter(getter.get(MatsimServices.class), drtConfigGroup.getMode(),
				new VehicleOccupancyProfileView(getter.getModal(VehicleOccupancyProfileCalculator.class), taskTypeComparator, taskTypePaints),
				"shift_occupancy_time_profiles"))).in(Singleton.class);

		addControlerListenerBinding().toProvider(modalProvider(getter -> new ProfileWriter(getter.get(MatsimServices.class), drtConfigGroup.getMode(),
				new VehicleTaskProfileView(getter.getModal(VehicleTaskProfileCalculator.class), taskTypeComparator, taskTypePaints),
				"shift_task_time_profiles"))).in(Singleton.class);

		bindModal(RegularShiftDump.class).toProvider(modalProvider(
				getter -> new RegularShiftDump(
						getter.getModal(new TypeLiteral<Provider<DrtShiftsSpecification>>(){}),
						getter.get(OutputDirectoryHierarchy.class)
				))
		).asEagerSingleton();

		addControlerListenerBinding().toProvider(modalProvider(
				getter -> getter.getModal(RegularShiftDump.class)
		));
	}
}
