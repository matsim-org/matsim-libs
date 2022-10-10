/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.run.DrtConfigGroup;

/**
 * @author mdziakowski / MOIA
 */
public class DrtWithOperationsConfigGroup extends DrtConfigGroup {

	@NotNull
	private DrtOperationsParams drtOperationsParams;

	public DrtWithOperationsConfigGroup() {
		addDefinition(DrtOperationsParams.SET_NAME, DrtOperationsParams::new,
				() -> drtOperationsParams,
				params -> drtOperationsParams = (DrtOperationsParams)params);
	}

	public DrtOperationsParams getDrtOperationsParams() {
		return drtOperationsParams;
	}
}
