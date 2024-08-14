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
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

public class LspShipmentWithTime {
  // yyyyyy find better solution for this.  It is not so good to define an interface, and then
  // immediately define a class that goes beyond it.
  // Maybe the time should be added to the interface?  However, I don't even know what that time
  // means (delivery time?  current time?).  kai,
  // jun'22

  private final LspShipment lspShipment;


  public LspShipmentWithTime(double time, LspShipment lspShipment) {
    this.lspShipment = lspShipment;
    LspShipmentUtils.setTimeOfLspShipment(this.lspShipment, time);
  }

  public LspShipment getLspShipment() {
    return lspShipment;
  }

  public double getTime() {
    return LspShipmentUtils.getTimeOfLspShipment(this.lspShipment);
//    return time;
  }

}
