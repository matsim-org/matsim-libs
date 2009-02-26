/* *********************************************************************** *
 * project: org.matsim.*
 * InformationExchanger.java
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

package playground.gregor.withindayevac.communication;

import java.util.HashMap;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;

public class InformationExchanger {
	
	
	private final NetworkLayer network;
	private final Map<Id, InformationStorage> informationStorages;

	public InformationExchanger(NetworkLayer network) {
		this.network = network;
		this.informationStorages = new HashMap<Id,InformationStorage>();
		init();
	}

	private void init() {
		for(Node node : this.network.getNodes().values()) {
			this.informationStorages.put(node.getId(), new InformationStorage());
		}
	}

	public InformationStorage getInformationStorage(Id id) {
		return this.informationStorages.get(id);
	}
	
}
;