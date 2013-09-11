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

package org.matsim.contrib.grips.view;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.matsim.contrib.grips.control.Controller;

public class DefaultSaveDialog extends JFileChooser
{
	private static final long serialVersionUID = 1L;
	protected Controller controller;
	public DefaultSaveDialog(Controller controller, final String fileExtension, final String fileDescription, boolean mandatory)
	{
		this.controller = controller;
		this.setFileFilter(new FileFilter()
		{
			
			@Override
			public String getDescription()
			{
				return fileDescription;
			}
			
			@Override
			public boolean accept(File f)
			{
				if (f.toString().endsWith(fileExtension))
					return true;
				else
					return false;
			}
		});
		this.setCurrentDirectory(controller.getCurrentWorkingDirectory());
	}

}
