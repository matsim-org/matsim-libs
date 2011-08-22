/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotWriterFactoryRegister
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;

public class SnapshotWriterFactoryRegister {

	/**
	 * The global register of snapshot writer factories, keyed by a unique
	 * identifier.
	 */
	private Map<String, SnapshotWriterFactory> factoryMap = new HashMap<String, SnapshotWriterFactory>();

	public SnapshotWriterFactory getInstance(String snapshotWriterType) {
		if (!factoryMap.containsKey(snapshotWriterType)) {
			throw new RuntimeException("Snapshot writer type " + snapshotWriterType
					+ " doesn't exist.");
		}
		return factoryMap.get(snapshotWriterType);
	}

	public void register(String string, SnapshotWriterFactory snapshotWriterFactory) {
		factoryMap.put(string, snapshotWriterFactory);
	}
	
}
