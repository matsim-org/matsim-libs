package org.matsim.contrib.drt.extension.shifts.run;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilityImpl;
import org.matsim.contrib.drt.extension.shifts.shift.*;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.modal.ModalProviders;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtQSimModule extends AbstractDvrpModeQSimModule {

	public ShiftDrtQSimModule(String mode) {
		super(mode);
	}

	@Override
	public void configureQSim() {
		bindModal(DrtShifts.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
			@Override
			public DrtShifts get() {
				DrtShiftsSpecification shiftsSpecification = getModalInstance(DrtShiftsSpecification.class);
				ImmutableMap<Id<DrtShift>, DrtShiftImpl> shifts = shiftsSpecification.getShiftSpecifications().values()
						.stream()
						.map(spec -> {
							DrtShiftBreakSpecification breakSpec = spec.getBreak();
							DefautShiftBreakImpl shiftBreak = new DefautShiftBreakImpl(
									breakSpec.getEarliestBreakStartTime(),
									breakSpec.getLatestBreakEndTime(),
									breakSpec.getDuration());
							return new DrtShiftImpl(spec.getId(), spec.getStartTime(), spec.getEndTime(), shiftBreak);
						})
						.collect(ImmutableMap.toImmutableMap(DrtShift::getId, s -> s));
				return () -> shifts;
			}
		}).asEagerSingleton();

		bindModal(OperationFacilities.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {

			@Override
			public OperationFacilities get() {
				OperationFacilitiesSpecification operationFacilitiesSpecification = getModalInstance(OperationFacilitiesSpecification.class);
				ImmutableMap<Id<OperationFacility>, OperationFacility> operationFacilities = operationFacilitiesSpecification.getOperationFacilitySpecifications().values()
						.stream()
						.map(spec -> (OperationFacility) new OperationFacilityImpl(
								spec.getId(), spec.getLinkId(), spec.getCoord(),
								spec.getCapacity(), spec.getCharger(), spec.getType()
						))
						.collect(ImmutableMap.toImmutableMap(OperationFacility::getId, s -> s));
				return () -> operationFacilities;
			}
		}).asEagerSingleton();
	}
}
