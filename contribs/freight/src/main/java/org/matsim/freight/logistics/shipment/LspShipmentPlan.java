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

package org.matsim.freight.logistics.shipment;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.HasBackpointer;

/**
 * The ordered history of a single {@link LspShipment}: the sequence of load/transport/unload/handling
 * elements that were applied to it during a scenario. Instances live inside an
 * {@link org.matsim.freight.logistics.LSPPlan}.
 */
public interface LspShipmentPlan extends HasBackpointer<Id<LspShipment>> {

  /** @return the id of the shipment this plan belongs to. */
  Id<LspShipment> getLspShipmentId();

  /** @return the plan elements recorded for the shipment, keyed by their id. */
  Map<Id<LspShipmentPlanElement>, LspShipmentPlanElement> getPlanElements();

  /**
   * Appends an element to the plan.
   *
   * @param id the id of the element
   * @param element the element to record
   */
  void addPlanElement(Id<LspShipmentPlanElement> id, LspShipmentPlanElement element);

  /**
   * @return the plan element scheduled to be added next, i.e. the most recent one by element orde
   *     (or null if the plan is empty).
   */
  LspShipmentPlanElement getMostRecentEntry();

  /** Removes all recorded elements from the plan. */
  void clear();
}
