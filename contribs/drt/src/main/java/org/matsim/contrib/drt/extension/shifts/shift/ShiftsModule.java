package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.shifts.analysis.*;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecificationImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftsModule extends AbstractDvrpModeModule {

	private DrtConfigGroup drtConfigGroup;
	private ShiftDrtConfigGroup shiftConfig;

	public ShiftsModule(String mode, DrtConfigGroup drtConfigGroup, ShiftDrtConfigGroup shiftConfig) {
		super(mode);
		this.drtConfigGroup = drtConfigGroup;
		this.shiftConfig = shiftConfig;
	}


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

	}
}
