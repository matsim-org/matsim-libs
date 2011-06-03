/* *********************************************************************** *
 * project: matsim
 * Initializable.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.framework;

/**
 * @author nagel
 *
 */
@Deprecated // IMHO, one should try to do without this interface until we really have a reason why it is necessary.  kai, jun'11
public interface Initializable {
	/**
	 * Design thoughts:<ul>
	 * <li> yyyy I don't like this "initialize" method that one can easily forget to call.
	 * And I am confident that one can do without it.  kai, may'10
	 * <p/>
	 * Moving the material of this method into the ctor of PersonDriverAgentImpl makes a larger number of tests fail.
	 * So it ain't that easy.  
	 * <p/> 
	 * I am thus extracting the interface from PlanAgent so that I can test separately in the initialization sequence
	 * if this interface is fulfilled.  Maybe not so great.  kai, jun'11
	 * </ul>
	 */
	public void initialize();
	


}
