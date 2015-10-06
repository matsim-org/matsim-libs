/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.view;

import org.matsim.contrib.evacuation.control.Controller;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * default open dialog
 * 
 * @author wdoering
 * 
 */
public class DefaultOpenDialog extends JFileChooser {
	/**
	 *
	 */
	private static final long serialVersionUID = -7361529399376514942L;
	protected Controller controller;

	public DefaultOpenDialog(Controller controller, final String fileExtension, final String fileDescription, boolean directory) {
		this.controller = controller;
		if (directory)
			this.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		else {
			this.setFileFilter(new FileNameExtensionFilter(fileDescription, fileExtension));
		}

		this.setCurrentDirectory(controller.getCurrentWorkingDirectory());
	}

	public void setCurrentDirectory(String dir) {
		super.setCurrentDirectory(new File(dir));
	}

}
