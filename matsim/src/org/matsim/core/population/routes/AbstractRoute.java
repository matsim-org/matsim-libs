/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.core.population.routes;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Route;
import org.matsim.core.basic.v01.BasicRouteImpl;
import org.matsim.core.network.LinkImpl;

/**
 * Default, abstract implementation of the {@link Route}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute extends BasicRouteImpl implements Route {

	private LinkImpl startLink = null;
	private LinkImpl endLink = null;
	
	/**
	 * This constructor is only needed for backwards compatibility reasons and thus is
	 * set to deprecated. New code should make use of the constructor which sets the
	 * start and the end link of a Route correctly.
	 */
	@Deprecated 
	protected AbstractRoute(){
	}
	
	public AbstractRoute(LinkImpl startLink, LinkImpl endLink) {
		super((startLink == null ? null : startLink.getId()), (endLink == null ? null : endLink.getId()));
		this.startLink = startLink;
		this.endLink = endLink;
	}

	public LinkImpl getEndLink() {
		return this.endLink;
	}

	public LinkImpl getStartLink() {
		return this.startLink;
	}

	public void setEndLink(final LinkImpl link) {
		this.endLink = link;
	}

	public void setStartLink(final LinkImpl link) {
		this.startLink = link;
	}

	@Override
	public Id getStartLinkId() {
		return (this.startLink == null ? null : this.startLink.getId());
	}

	@Override
	public Id getEndLinkId() {
		return (this.endLink == null ? null : this.endLink.getId());
	}

}
