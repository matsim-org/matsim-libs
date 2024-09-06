package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

public class OperationFacilitiesModeModule extends AbstractDvrpModeModule {

	private final OperationFacilitiesParams operationFacilitiesParams;

	public OperationFacilitiesModeModule(DrtWithExtensionsConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.operationFacilitiesParams = drtCfg.getDrtOperationsParams().orElseThrow().getOperationFacilitiesParams().orElseThrow();
	}

	@Override
	public void install() {
		if (operationFacilitiesParams.operationFacilityInputFile != null) {
			bindModal(OperationFacilitiesSpecification.class).toProvider(() -> {
				OperationFacilitiesSpecification operationFacilitiesSpecification = new OperationFacilitiesSpecificationImpl();
				new OperationFacilitiesReader(operationFacilitiesSpecification)
						.readURL(operationFacilitiesParams.getOperationFacilityInputUrl(getConfig().getContext()));
				return operationFacilitiesSpecification;
			}).asEagerSingleton();
		}
	}
}
