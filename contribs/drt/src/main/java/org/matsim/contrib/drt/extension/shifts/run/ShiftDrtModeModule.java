package org.matsim.contrib.drt.extension.shifts.run;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.shifts.analysis.*;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecificationImpl;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftsSpecificationImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;

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

		this.installQSimModule(new ShiftDrtQSimModule(getMode()));
    }
}
