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

package org.matsim.population.routes;

import org.matsim.basic.v01.BasicRouteImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Route;

/**
 * Default, abstract implementation of the {@link Route}-interface.
 *
 * @author mrieser
 */
public abstract class AbstractRoute extends BasicRouteImpl implements Route {

	private Link startLink = null;
	private Link endLink = null;
	
	/**
	 * This constructor is only needed for backwards compatibility reasons and thus is
	 * set to deprecated. New code should make use of the constructor which sets the
	 * start and the end link of a Route correctly.
	 */
	@Deprecated 
	protected AbstractRoute(){
	}
	
	public AbstractRoute(Link startLink, Link endLink) {
		super((startLink == null ? null : startLink.getId()), (endLink == null ? null : endLink.getId()));
		this.startLink = startLink;
		this.endLink = endLink;
	}

	public Link getEndLink() {
		return this.endLink;
	}

	public Link getStartLink() {
		return this.startLink;
	}

	public void setEndLink(final Link link) {
		this.endLink = link;
	}

	public void setStartLink(final Link link) {
		this.startLink = link;
	}

	@Override 
	@Deprecated
	public void setStartLinkId(final Id linkId) {
		throw new UnsupportedOperationException("Please use setStartLink(Link). Setting the link-id is only supported for BasicRoute, but not higher in the class hierarchy.");
	}

	@Override
	public Id getStartLinkId() {
		return (this.startLink == null ? null : this.startLink.getId());
	}

	@Override
	public void setEndLinkId(final Id linkId) {
		throw new UnsupportedOperationException("Please use setEndLink(Link). Setting the link-id is only supported for BasicRoute, but not higher in the class hierarchy.");
	}

	@Override
	@Deprecated
	public Id getEndLinkId() {
		return (this.endLink == null ? null : this.endLink.getId());
	}

}
