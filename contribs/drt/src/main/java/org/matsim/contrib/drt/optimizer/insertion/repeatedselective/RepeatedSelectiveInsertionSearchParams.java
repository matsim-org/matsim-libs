/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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


package org.matsim.contrib.drt.optimizer.insertion.repeatedselective;

import jakarta.validation.constraints.Positive;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;

/**
 * @author steffenaxer
 */
public class RepeatedSelectiveInsertionSearchParams extends DrtInsertionSearchParams {
	public static final String SET_NAME = "RepeatedSelectiveInsertionSearch";

	@Parameter
	@Positive
	public int retryInsertion = 5;

	public RepeatedSelectiveInsertionSearchParams() {
		super(SET_NAME);
	}
}
