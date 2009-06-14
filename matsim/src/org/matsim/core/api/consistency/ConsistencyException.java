/* *********************************************************************** *
 * project: org.matsim.*
 * ConsistencyException
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.api.consistency;

import java.util.List;


/**
 * Is a well suited class already existing in java? Probably something similar or we could
 * use the more general IllegalStateException. Idea behind this class would be
 * to use it toghether with a container in the ConsistencyChecker Interface that will store
 * all occured consistency exceptions.
 * 
 * Should this class extend Runtime or CheckedException?
 * 
 * @author dgrether
 * @deprecated just a draft to be discussed, don't implement this interface yet.
 */
@Deprecated
public abstract class ConsistencyException extends Exception {

	public abstract List<Exception> getExceptions();
	
}
