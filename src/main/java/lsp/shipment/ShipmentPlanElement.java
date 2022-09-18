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

package lsp.shipment;

import lsp.LSPResource;
import lsp.LogisticsSolutionElement;
import org.matsim.api.core.v01.Id;

public interface ShipmentPlanElement {

	LogisticsSolutionElement getSolutionElement();

	Id<LSPResource> getResourceId();

	// yyyy "type" feels like this makes it a tagged class.  These should be avoided (Effective Java 2018, Item 23).  It is, however, probably not
	// used as a type, but rather as a description.  Rename?
	String getElementType();

	double getStartTime();

	double getEndTime();

}
