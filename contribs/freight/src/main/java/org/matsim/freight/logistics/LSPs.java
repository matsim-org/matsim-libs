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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * A container that stores {@link LSP}s.
 */
public class LSPs {

	private static final Logger log = LogManager.getLogger(LSPs.class);
	private final Map<Id<LSP>, LSP> lsps = new LinkedHashMap<>();

	public LSPs() {}

	public LSPs(Collection<LSP> lsps) {
		addLsps(lsps);
	}

	public void addLsps(Collection<LSP> lsps) {
		for (LSP lsp : lsps) {
			addLsp(lsp);
		}
	}
	public void addLsp(LSP lsp) {
		if (!lsps.containsKey(lsp.getId())) {
			lsps.put(lsp.getId(), lsp);
		}
		else log.warn("LSP {} already exists. It has NOT been added.", lsp.getId());
	}

	public Map<Id<LSP>, LSP> getLSPs() {
		return lsps;
	}
}
