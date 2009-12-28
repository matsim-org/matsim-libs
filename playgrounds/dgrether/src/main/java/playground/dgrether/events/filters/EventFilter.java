/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.events.filters;

import org.matsim.core.events.PersonEventImpl;


/**
 * @author dgrether
 *
 */
public interface EventFilter {
	/**
	 * judges whether the PersonEvent
	 * ({@link org.matsim.core.events.PersonEventImpl}) will be processed or not
	 *
	 * @param event -
	 *            which is being judged
	 * @return true if the event meets the criterion of the implementation
	 */
	boolean judge(PersonEventImpl event);


}
