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

import java.util.Collection;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * Each LogisticsSolutionElement maintains two collections of WaitingShipments. Instances of the
 * latter class contain tuples of LSPShipments and time stamps.
 *
 * <p>The first of these collections stores LSPShipments that are waiting for their treatment in
 * this element or more precisely the Resource that is in charge of the actual physical handling.
 *
 * <p>The second one stores shipments that have already been treated.
 *
 * <p>At the beginning of the scheduling process, all LSPShipments are added to the collection of
 * incoming shipments of the first LogisticsSolutionElement of the LogisticsSolution to which they
 * were assigned before. The tuples in the collection of WaitingShipments thus consist of the
 * shipments themselves and a time stamp that states when they arrived there (see 3.9). In the case
 * of the first LogisticsSolutionElement, this time stamp corresponds to the start time window of
 * the LSPShipment
 */
public interface WaitingShipments {

  void addShipment(double time, LspShipment lspShipment);

  Collection<LspShipment> getSortedLspShipments();

  Collection<LspShipment> getLspShipmentsWTime();

  void clear();
}
