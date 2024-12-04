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

import java.util.*;
import org.matsim.api.core.v01.Id;

/*package-private*/ class LspShipmentPlanImpl implements LspShipmentPlan {

  private final Id<LspShipment> lspShipmentId;
  private final LinkedHashMap<Id<LspShipmentPlanElement>, LspShipmentPlanElement> planElements;

  LspShipmentPlanImpl(Id<LspShipment> lspShipmentId) {
    this.lspShipmentId = lspShipmentId;
    this.planElements = new LinkedHashMap<>();
  }

  // TODO: Ist kein embedding container!
  @Override
  public void setEmbeddingContainer(Id<LspShipment> pointer) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public Id<LspShipment> getLspShipmentId() {
    return lspShipmentId;
  }

  @Override
  public void addPlanElement(Id<LspShipmentPlanElement> id, LspShipmentPlanElement element) {
    planElements.put(id, element);
  }

  @Override
  public Map<Id<LspShipmentPlanElement>, LspShipmentPlanElement> getPlanElements() {
    return Collections.unmodifiableMap(planElements);
  }

  @Override
  public LspShipmentPlanElement getMostRecentEntry() {

    // there is no method to remove entries.  in consequence, the only way to change the result of
    // this method is to "add" additional material into the plan.  Possibly,
    // the method here is indeed there to find the plan element that was added most recently, to
    // figure out how the next one can be added.  However, this then
    // should be sorted by sequence of addition, not by timing.  ???   kai/kai, apr'21

    ArrayList<LspShipmentPlanElement> logList = new ArrayList<>(planElements.values());
    logList.sort(new LogElementComparator());
    Collections.reverse(logList);
    return logList.getFirst();
  }

  @Override
  public void clear() {
    planElements.clear();
  }

  static class LogElementComparator implements Comparator<LspShipmentPlanElement> {

    @Override
    public int compare(LspShipmentPlanElement o1, LspShipmentPlanElement o2) {
      if (o1.getStartTime() > o2.getStartTime()) {
        return 1;
      }
      if (o1.getStartTime() < o2.getStartTime()) {
        return -1;
      }
      if (o1.getStartTime() == o2.getStartTime()) {
        if (o1.getEndTime() > o2.getEndTime()) {
          return 1;
        }
        if (o1.getEndTime() < o2.getEndTime()) {
          return -1;
        }
      }
      return 0;
    }
  }
}
