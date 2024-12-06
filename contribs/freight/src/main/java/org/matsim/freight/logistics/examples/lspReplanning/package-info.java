/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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

/**
 * @deprecated  This package is deprecated and will be removed in the future.
 *  It follows the old and no longer wanted approach.
 *  Now, an Assigner is used to assign all LSPShipments to one LogisticChain of the LSPPlan.
 *  <p></p>
 *  This class here is in contrast used as a Replanning strategy. This behavior is not wanted anymore.
 *  <p></p>
 *  Please use the new approach as shown in
 *  {@link org.matsim.freight.logistics.examples.multipleChains} ExampleMultipleOneEchelonChainsReplanning instead.
 *  ((The old approach is still available in CollectionLSPReplanningTest but will get removed earlier or later)).
 *  <p></p>
 *  KMT, Jul'24
 * */
@Deprecated
package org.matsim.freight.logistics.examples.lspReplanning;
