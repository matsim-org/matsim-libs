/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations;

import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public class DrtOperationsParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String SET_NAME = "drtOperations";

	@Nullable
	private ShiftsParams shiftsParams;

	@Nullable
	private OperationFacilitiesParams operationFacilitiesParams;

	public DrtOperationsParams() {
        super(SET_NAME);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//shifts (optional)
		addDefinition(ShiftsParams.SET_NAME, ShiftsParams::new, () -> shiftsParams,
				params -> shiftsParams = (ShiftsParams) params);
		//operationFacilities (optional)
		addDefinition(OperationFacilitiesParams.SET_NAME, OperationFacilitiesParams::new, () -> operationFacilitiesParams,
				params -> operationFacilitiesParams = (OperationFacilitiesParams) params);
	}

	public Optional<ShiftsParams> getShiftsParams() {
		return Optional.ofNullable(shiftsParams);
	}

	public Optional<OperationFacilitiesParams> getOperationFacilitiesParams() {
		return Optional.ofNullable(operationFacilitiesParams);
	}
}
