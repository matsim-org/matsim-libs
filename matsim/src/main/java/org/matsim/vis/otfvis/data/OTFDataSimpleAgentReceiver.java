/* *********************************************************************** *
 * project: org.matsim.*
 * Receiver.java
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

/**
 * 
 */
package org.matsim.vis.otfvis.data;

public interface OTFDataSimpleAgentReceiver extends OTFDataReceiver{
	
	/**
	 * @param id
	 * @param startX
	 * @param startY
	 * @param state ... is ignored in most if not all places.  kai, jan'10
	 * @param userdefined
	 * @param color
	 * 
	 * <p>
	 * yyyy My intuition is that we should try to remove both "state" and "userdefined".  If this is not possible
	 * for backwards compatibility reasons, then the old setAgent should be set deprecated, and there should 
	 * be a newer one, with shorter signature.  kai, jan'10
	 * </p>
	 */
	public void setAgent(char[] id, float startX, float startY, int state, int userdefined, float color);

}
