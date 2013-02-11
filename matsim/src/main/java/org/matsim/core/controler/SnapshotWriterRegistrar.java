/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotWriterRegistrar
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

import org.apache.log4j.Logger;
import org.matsim.vis.snapshotwriters.KMLSnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.TransimsSnapshotWriterFactory;

public class SnapshotWriterRegistrar {

	private final static Logger log = Logger.getLogger(SnapshotWriterRegistrar.class);
	
	private SnapshotWriterFactoryRegister register = new SnapshotWriterFactoryRegister();

	public SnapshotWriterRegistrar() {
		register.register("googleearth", new KMLSnapshotWriterFactory());
		register.register("transims", new TransimsSnapshotWriterFactory());
		try {
			Class<?> klass = this.getClass().getClassLoader().loadClass("org.matsim.vis.otfvis.OTFFileWriterFactory");
			register.register("otfvis", (SnapshotWriterFactory) klass.newInstance());
		} catch (ClassNotFoundException e) {
			log.info("OTFVis snapshots will not be supported, as OTFVis is not part of the classpath.");
		} catch (InstantiationException e) {
			log.error("Could not register OTFVis snapshot writer, despite the class being available.");
		} catch (IllegalAccessException e) {
			log.error("Could not register OTFVis snapshot writer, despite the class being available.");
		}
	}
	
	public SnapshotWriterFactoryRegister getFactoryRegister() {
		return register;
	}

}
