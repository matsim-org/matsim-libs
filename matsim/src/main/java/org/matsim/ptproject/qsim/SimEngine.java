/* *********************************************************************** *
 * project: org.matsim.*
 * SimEngine
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


/**
 * @author dgrether
 * @TODO rename QSimEngine to QSimNetorkEngine and this to QSimEngine
 */
public interface SimEngine {
  /**
   * 
   * @return the QSim instance
   */
  public QSim getQSim();
  /**
   * called in a predefined Order when the simulation is started
   */
  public void onPrepareSim();
  /**
   * Do some clean up.
   */
  public void afterSim();
  
}
