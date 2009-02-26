/* *********************************************************************** *
 * project: org.matsim.*
 * Node.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.interfaces.core.v01;

import java.util.Map;

import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.interfaces.basic.v01.Id;

public interface Node extends BasicNode, Comparable<Node> {

	public void setOrigId(final String id);

	@Deprecated
	public void setTopoType(final int topotype);

	public void setType(final String type);

	public void removeInLink(final Link inlink);

	public void removeOutLink(final Link outlink);

	public String getOrigId();

	public String getType();

	public Map<Id, ? extends Link> getIncidentLinks();

	public Map<Id, ? extends Node> getInNodes();

	public Map<Id, ? extends Node> getOutNodes();

	public Map<Id, ? extends Node> getIncidentNodes();

	@Deprecated
	public int getTopoType();

	public Map<Id, ? extends Link> getInLinks();

	public Map<Id, ? extends Link> getOutLinks();

}
