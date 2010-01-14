/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLink
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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface QueueLink {

  /**
   *  Is called after link has been read completely 
   * @deprecated implementation not doing anything
   */
  @Deprecated
  public void finishInit();

  public void setSimEngine(final QueueSimEngine simEngine);

  public void activateLink();

  public Link getLink();

  
  /**
   * Adds a vehicle to the link, called by
   * {@link QueueNode#moveVehicleOverNode(QueueVehicle, QueueLane, double)}.
   *
   * @param veh
   *          the vehicle
   */
  public void add(final QueueVehicle veh);

  public void addParkedVehicle(QueueVehicle vehicle);

  public QueueVehicle getVehicle(Id vehicleId);

  public Collection<QueueVehicle> getAllVehicles();
  
  public void recalcTimeVariantAttributes(double time);

  /**
   * @return the total space capacity available on that link (includes the space on lanes if available)
   */
  public double getSpaceCap();

//  public Queue<QueueVehicle> getVehiclesInBuffer();

  /**
   * This method returns the normalized capacity of the link, i.e. the capacity
   * of vehicles per second. It is considering the capacity reduction factors
   * set in the config and the simulation's tick time.
   *
   * @return the flow capacity of this link per second, scaled by the config
   *         values and in relation to the SimulationTimer's simticktime.
   */
  public double getSimulatedFlowCapacity();

  public VisData getVisData();

  
  // methods that have been marked as package or protected
  // before the interface was introduced
  
  public QueueNode getToQueueNode();
  
  public QueueNetwork getQueueNetwork();

  public boolean hasSpace();
 
  public boolean bufferIsEmpty();
  
  public void clearVehicles();
  
  public boolean moveLink(double now);
  
  public QueueVehicle removeParkedVehicle(Id vehicleId);
  
  /**
   * @deprecated can be removed see implementation
   * @param now
   * @param veh
   */
  @Deprecated
  public void processVehicleArrival(final double now, final QueueVehicle veh);
  
  public void addDepartingVehicle(QueueVehicle vehicle);
}