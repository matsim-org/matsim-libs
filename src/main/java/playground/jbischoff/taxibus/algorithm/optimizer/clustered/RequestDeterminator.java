/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.algorithm.optimizer.clustered;

import java.util.List;
import java.util.Set;

import org.matsim.contrib.av.drt.TaxibusRequest;
import org.matsim.contrib.dvrp.data.Request;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public interface RequestDeterminator {

	boolean isRequestServable(Request request);

	/**
	 * @param requests
	 * @return
	 */
	List<Set<TaxibusRequest>> prefilterRequests(Set<TaxibusRequest> requests);
	
}
