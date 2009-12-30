/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLiveFileSettingsSaver
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
package org.matsim.vis.otfvis.opengl.gui;

import java.io.File;

import org.matsim.vis.otfvis.gui.OTFVisConfig;


/**
 * @author dgrether
 *
 */
public class OTFLiveSettingsSaver extends OTFAbstractSettingsSaver {

	public OTFLiveSettingsSaver(OTFVisConfig visconf, String filename) {
		super(visconf, filename);
	}

	
	public void readDefaultSettings() {
		File file = new File(fileName + ".vcfg");
		if(file.exists())openAndReadConfigFromFile(file);
	}
}
