/* *********************************************************************** *
 * project: org.matsim.*
 * QBufferItem
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.Id;



/**
 * Interface representing the buffer functionality common for all Queue-Logic Links and Lanes, i.e.
 * providing selected, decorated methods for Buffer access and additional methods needed for
 * the buffer logic implemented.
 * @author dgrether
 *
 */
interface QBufferItem extends QSimFunctionalInterface {
  /**
   * equivalent to a Buffer.isEmpty() operation 
   */
  boolean bufferIsEmpty();

  /**
   * equivalent to a Buffer.pop() operation 
   */
  QVehicle popFirstFromBuffer();
  /**
   * equivalent to a Buffer.peek() operation 
   */
  QVehicle getFirstFromBuffer();
  
  double getBufferLastMovedTime();

	boolean hasGreenForToLink(Id toLinkId);
  
}
