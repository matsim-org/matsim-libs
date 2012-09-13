/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.mzilske.osm;

import javax.swing.JPanel;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Tile;

public class MyJMapViewer extends JMapViewer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel panel;

	public MyJMapViewer(JPanel compositePanel) {
		this.panel = compositePanel;
	}

	@Override
	public void tileLoadingFinished(Tile tile, boolean success) {
		super.tileLoadingFinished(tile, success);
		// We need to notify our parent component that we are finished drawing tiles, since
		// our parent component is probably an overlay over this map, which needs to be redrawn now.
		panel.repaint();
	}
	
	

}
