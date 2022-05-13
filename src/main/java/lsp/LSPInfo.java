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

package lsp;

import lsp.shipment.LSPShipment;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.HashSet;
import java.util.Set;

/**
 * In order to enable a wide range of data to be represented, the abstract class {@link LSPInfo} was created.
 * It contains data about the object to which it is attached.
 *
 * Infos can be attached to {@link LogisticsSolution}s, {@link LSPShipment}s, {@link LogisticsSolutionElement}s and {@link LSPResource}s.
 *
 * Further, they can be valid only during a certain period of time.
 *
 * Note (KMT, KN Mrz22): If we need to store more infos inside an attribute, e.g. a Collection for the {@link CostInfo}:
 * There are the AttributeConverter(s) in MATSim-libs.
 * One could easily create a DoubleCollectionConverter based on the StringCollectionConverter if needed.
 */
public abstract class LSPInfo implements Attributable {

	private final Attributes attributes = new Attributes();
	
	protected LSPInfo() {
	}

	public abstract void setName(String name);
	public abstract String getName();
	public abstract void update();

	@Override
	public Attributes getAttributes(){
		return attributes;
	}


}
