/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.extension;

import org.matsim.contrib.drt.extension.companions.DrtCompanionParams;
import org.matsim.contrib.drt.extension.insertion.spatialFilter.DrtSpatialRequestFleetFilterParams;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.services.services.params.DrtServicesParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.run.DrtConfigGroup;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Steffen Axer
 * <p>
 * This class summarizes all optional drt parametersets and should be used while creating MultiModeDrtConfigGroup instances
 */
public class DrtWithExtensionsConfigGroup extends DrtConfigGroup {

	@Nullable
	private DrtCompanionParams drtCompanionParams;

	@Nullable
	private DrtOperationsParams drtOperationsParams;

	@Nullable
	private DrtServicesParams drtServicesParams;

	@Nullable
	private DrtSpatialRequestFleetFilterParams drtSpatialRequestFleetFilterParams;

	public DrtWithExtensionsConfigGroup() {
		this(DefaultDrtOptimizationConstraintsSet::new);
	}

	public DrtWithExtensionsConfigGroup(Supplier<DrtOptimizationConstraintsSet> drtOptimizationConstraintsSetSupplier) {
		super(drtOptimizationConstraintsSetSupplier);
		// Optional
		addDefinition(DrtCompanionParams.SET_NAME, DrtCompanionParams::new, () -> drtCompanionParams,
			params -> drtCompanionParams = (DrtCompanionParams) params);

		// Optional
		addDefinition(DrtOperationsParams.SET_NAME, DrtOperationsParams::new, () -> drtOperationsParams,
			params -> drtOperationsParams = (DrtOperationsParams) params);

		// Optional
		addDefinition(DrtServicesParams.SET_TYPE, DrtServicesParams::new, () -> drtServicesParams,
			params -> drtServicesParams = (DrtServicesParams) params);

		// Optional
		addDefinition(DrtSpatialRequestFleetFilterParams.SET_NAME, DrtSpatialRequestFleetFilterParams::new, () -> drtSpatialRequestFleetFilterParams,
			params -> drtSpatialRequestFleetFilterParams = (DrtSpatialRequestFleetFilterParams) params);
	}

	public Optional<DrtCompanionParams> getDrtCompanionParams() {
		return Optional.ofNullable(drtCompanionParams);
	}

	public Optional<DrtOperationsParams> getDrtOperationsParams() {
		return Optional.ofNullable(drtOperationsParams);
	}

	public Optional<DrtServicesParams> getServicesParams() {
		return Optional.ofNullable(drtServicesParams);
	}

	public Optional<DrtSpatialRequestFleetFilterParams> getSpatialRequestFleetFilterParams() {
		return Optional.ofNullable(drtSpatialRequestFleetFilterParams);
	}
}
