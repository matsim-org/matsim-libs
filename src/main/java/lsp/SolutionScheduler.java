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


/**
 * Serve the purpose of routing a set of {@link lsp.shipment.LSPShipment}s through a set of
 * {@link LogisticsSolution}s, which, in turn, consist of several {@link LogisticsSolutionElement}s
 * and the corresponding {@link LSPResource}s.
 */
public interface SolutionScheduler {

	void scheduleSolutions();

	void setLSP(LSP lsp);
	
	void setBufferTime(int bufferTime);
}
