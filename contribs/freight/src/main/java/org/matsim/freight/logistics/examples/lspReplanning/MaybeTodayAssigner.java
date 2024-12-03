/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
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
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.lspReplanning;

import java.util.Random;
import org.matsim.core.gbl.Gbl;
import org.matsim.freight.logistics.InitialShipmentAssigner;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
* This class is deprecated and will be removed in the future.
 * It follows the old and no longer wanted approach.
 * Now, an Assigner is used to assign all LSPShipments to one LogisticChain of the LSPPlan.
 * <p></p>
 * This class here is in contrast used as a Replanning strategy. This behavior is not wanted anymore.
 * KMT, Jul'24
*/
@Deprecated
/*package-private*/ class MaybeTodayAssigner implements InitialShipmentAssigner {

  private final Random random;

  public MaybeTodayAssigner() {
    this.random = new Random(1);
  }

  @Override
  public void assignToPlan(LSPPlan lspPlan, LspShipment lspShipment) {
    boolean assignToday = random.nextBoolean();
    if (assignToday) {
      Gbl.assertIf(lspPlan.getLogisticChains().size() == 1);
      lspPlan.getLogisticChains().iterator().next().addShipmentToChain(lspShipment);
    }
  }

}
