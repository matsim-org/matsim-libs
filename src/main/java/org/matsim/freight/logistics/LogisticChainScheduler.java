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

/**
 * Serve the purpose of routing a set of {@link LspShipment}s
 * through a set of {@link LogisticChain}s, which, in turn, consist of several {@link
 * LogisticChainElement}s and the corresponding {@link LSPResource}s.
 */
public interface LogisticChainScheduler extends HasBackpointer<LSP> {

  void scheduleLogisticChain();

  /**
   * The buffer time is <b>only taken into account in planning / scheduling</b>. The idea is to
   * ensure that the goods are available for the next ressource "in time", because the scheduling
   * does not take into account any congestion during the simulation. E.g. if multiple vehicle are
   * leaving the depot at the same time and thus influence each other.<br>
   * It is <b> not </b> intended to be available as buffer in the simulation itself -> It does not
   * influence the events and shipmentLogs. As a consequence, the transportation (in simulation,
   * events, ...) is in many cases earlier than scheduled. (Information from TM after asking; KMT
   * 17.11.23)
   *
   * @param bufferTime for scheduling [in sec]
   */
  void setBufferTime(int bufferTime);
}
