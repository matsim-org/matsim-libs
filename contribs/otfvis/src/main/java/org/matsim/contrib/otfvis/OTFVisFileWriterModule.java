/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * OTFVisModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.contrib.otfvis;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class OTFVisFileWriterModule extends AbstractModule {
	@Override
	public void install() {
		ConfigUtils.addOrGetModule(getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		if (getConfig().controler().getSnapshotFormat().contains("otfvis")) {
			addSnapshotWriterBinding().toProvider(OTFFileWriterFactory.class);
		}
	}
}
