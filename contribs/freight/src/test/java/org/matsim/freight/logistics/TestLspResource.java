/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Minimal {@link LSPResource} for unit tests. It does nothing when scheduled, i.e. the shipments stay in the
 * incoming shipments of its client elements.
 */
final class TestLspResource extends LSPDataObject<LSPResource> implements LSPResource {
	private final Collection<LogisticChainElement> clientElements = new ArrayList<>();

	TestLspResource(Id<LSPResource> id) {
		super(id);
	}

	@Override
	public Id<Link> getStartLinkId() {
		return null;
	}

	@Override
	public Id<Link> getEndLinkId() {
		return null;
	}

	@Override
	public Collection<LogisticChainElement> getClientElements() {
		return clientElements;
	}

	@Override
	public void schedule(int bufferTime, LSPPlan lspPlan) {
	}
}
