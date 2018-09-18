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

import java.util.Collections;
import java.util.Set;

import org.matsim.contrib.drt.data.DrtRequest;

/**
 * Accepts all DRT requests as long as the start and end link are different.
 *
 * @author jbischoff
 */
public class DefaultDrtRequestValidator implements DrtRequestValidator {
	public static final String EQUAL_FROM_LINK_AND_TO_LINK_CAUSE = "eqal_fromLink_and_toLink";

	@Override
	public Set<String> validateDrtRequest(DrtRequest request) {
		return request.getFromLink() == request.getToLink() ?
				Collections.singleton(EQUAL_FROM_LINK_AND_TO_LINK_CAUSE) :
				Collections.emptySet();
	}
}
