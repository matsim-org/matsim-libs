/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics;

import org.matsim.freight.logistics.shipment.LspShipment;

public class LspShipmentWithTime {
  // yyyyyy find better solution for this.  It is not so good to define an interface, and then
  // immediately define a class that goes beyond it.
  // Maybe the time should be added to the interface?  However, I don't even know what that time
  // means (delivery time?  current time?).  kai,
  // jun'22

  private final LspShipment lspShipment;
//  private final double time;

  public LspShipmentWithTime(double time, LspShipment lspShipment) {
    this.lspShipment = lspShipment;
    setTimeOfLspShipment(this.lspShipment, time);
//    this.time = time;
  }

  public LspShipment getLspShipment() {
    return lspShipment;
  }

  public double getTime() {
    return getTimeOfLspShipment(this.lspShipment);
//    return time;
  }

  /**
   * Stores a time as Attribute in the LspShipment.
   * This is needed for some kind of tracking the shipment.
   * <p>
   * This will replace the LSPShipmentWithTime class and thus reduce the complexity of the code.
   * KMT Jul'24
   * @param lspShipment the LspShipment to store the time in
   * @param time the time to store
   */
  private void setTimeOfLspShipment(LspShipment lspShipment, double time){
    lspShipment.getAttributes().putAttribute("time", time);
  }

  /**
   * Returns the time stored in the LspShipment.
   * <p>
   * This will replace the LSPShipmentWithTime class and thus reduce the complexity of the code. KMT Jul'24
   * @param lspShipment the LspShipment to get the time from
   * @return the time as double
   */
  private double getTimeOfLspShipment(LspShipment lspShipment) {
    return (double) lspShipment.getAttributes().getAttribute("time");
  }
}
