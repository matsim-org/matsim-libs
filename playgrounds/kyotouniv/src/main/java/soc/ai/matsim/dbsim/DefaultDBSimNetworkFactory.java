/* *********************************************************************** *
 * project: org.matsim.*
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

package soc.ai.matsim.dbsim;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;


/**
 * @author dgrether
 */
/*package*/ final class DefaultDBSimNetworkFactory implements DBSimNetworkFactory<DBSimNode, DBSimLink> {

	@Override
	public DBSimLink newQueueLink(final Link link, final DBSimNetwork queueNetwork, final DBSimNode toQueueNode) {
		return new DBSimLink(link, queueNetwork, toQueueNode);
	}

	@Override
	public DBSimNode newQueueNode(final Node node, final DBSimNetwork dbsimNetwork) {
		return new DBSimNode(node, dbsimNetwork);
	}

}
