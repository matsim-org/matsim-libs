/* *********************************************************************** *
 * project: org.matsim.*
 * ConsistencyCheckerManager
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
package playground.dgrether.consistency;

import java.util.List;


/**
 * Classes implementing this interface are responsible to store 
 * several instances ConsistencyChecker type.
 * Furthermore it has to provide a method to call all registered consistencyCheckers
 * @author dgrether
 * @deprecated just a draft to be discussed, don't implement this interface yet.
 */
@Deprecated
public interface ConsistencyCheckerManager {

	public List<ConsistencyChecker> getConsistencyCheckers();
		
	public void checkConsistency();

}
