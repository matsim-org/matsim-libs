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

/**
 * 
 */
package org.matsim.contrib.taxibus.algorithm.optimizer.prebooked;

import java.util.*;

import org.matsim.contrib.taxibus.TaxibusRequest;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public interface RequestFilter {
	/**
	 * @param requests
	 * @return
	 */
	List<Set<TaxibusRequest>> prefilterRequests(Set<TaxibusRequest> requests);
}
