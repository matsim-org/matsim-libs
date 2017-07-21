/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.data.validator;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.data.DrtRequest;

/**
 * Accepts all DRT requests as long as start and end link are different.
 * 
 * @author jbischoff
 */
public class DefaultDrtRequestValidator implements DrtRequestValidator {
	@Override
	public boolean validateDrtRequest(DrtRequest request) {
		if (request.getFromLink() == request.getToLink()) {
			// throw new IllegalArgumentException("fromLink and toLink must be different");
			Logger.getLogger(getClass()).error("fromLink and toLink must be different. Request " + request.getId()
					+ " will not be served. The agent will stay in limbo.");
			return false;
		}

		return true;
	}
}
