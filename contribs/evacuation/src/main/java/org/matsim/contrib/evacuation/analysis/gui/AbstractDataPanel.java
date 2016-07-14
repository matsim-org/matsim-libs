/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package org.matsim.contrib.evacuation.analysis.gui;

import javax.swing.JPanel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.analysis.data.EventData;

public abstract class AbstractDataPanel extends JPanel implements DataPanelInterface {

	private static final long serialVersionUID = 1L;
	protected EventData<Link> data;
	protected int width;
	protected int height;

	public AbstractDataPanel() {
		// reset all values
		resetData();
	}

	@Override
	public void resetData() {
		data = null;
	}

	@Override
	public void updateData(EventData data) {
		this.data = data;
		drawDataPanel();
	}

	@Override
	public void setPanelSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public abstract void drawDataPanel();

}
