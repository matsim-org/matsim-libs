/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRendererStuck.java
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

package org.matsim.utils.vis.netvis.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.JFrame;

import org.matsim.events.EventAgentStuck;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.utils.vis.netvis.DisplayableLinkI;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;

public class LinkSetRendererStuck extends RendererA {

	private final static int MODE_LINKS = 1;
	private final static int MODE_VEHICLES = 2;

	ControlToolbar toolbar = null;
	private final int mode = MODE_VEHICLES;

	private final TreeMap<String, ArrayList<Double>> stuckTimes = new TreeMap<String, ArrayList<Double>>();

//	private final ValueColorizer colorizer = new ValueColorizer(new double[] { 0*3600, 6*3600, 12*3600, 18*3600, 24*3600 }, new Color[] {
//			Color.CYAN, Color.RED, Color.YELLOW, Color.GREEN, Color.GRAY });
	private final ValueColorizer colorizerLinks = new ValueColorizer(new double[] { 0, 10, 20, 100}, new Color[] {
			Color.WHITE, Color.YELLOW, Color.RED, Color.BLUE });
	private final ValueColorizer colorizerVehicles = new ValueColorizer(new double[] { 0, 6*3600, 12*3600, 18*3600, 24*3600 },
			new Color[] {Color.BLUE, Color.RED, Color.ORANGE, Color.YELLOW, Color.WHITE});

	private final DisplayNet network;

	private double linkWidth;
	private final int extraWidth = 5;


	public LinkSetRendererStuck(final VisConfig visConfig, final DisplayNet network) {
		super(visConfig);
		this.network = network;

		this.linkWidth = DisplayLink.LANE_WIDTH * visConfig.getLinkWidthFactor();
		chooseCountComparison();
	}

	public void chooseCountComparison() {
		String filename = "";

		JFrame tmpFrame = new JFrame("");
		FileDialog dialog = new FileDialog(tmpFrame, "Choose an events-file", FileDialog.LOAD);
		dialog.setVisible(true);

		while (dialog.getFile() != null)	{
			filename = dialog.getDirectory()+"/"+dialog.getFile();

			Events events = new Events();
			StuckEventCollector collector = new StuckEventCollector();
			events.addHandler(collector);
			new MatsimEventsReader(events).readFile(filename);
			events.printEventsCount();
			return;
		}
		tmpFrame.dispose();
	}

	@Override
	public
	void setTargetComponent(final NetJComponent comp) {
		super.setTargetComponent(comp);
	}

	// -------------------- RENDERING --------------------

	@Override
	protected
	synchronized void myRendering(final Graphics2D display, final AffineTransform boxTransform) {
		this.linkWidth = DisplayLink.LANE_WIDTH* getVisConfig().getLinkWidthFactor();

		NetJComponent comp = getNetJComponent();

		AffineTransform originalTransform = display.getTransform();

		double scale = 1.0;
		if (this.toolbar != null) {
			scale = this.toolbar.getScale();
		}

		//display.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double strokeWidth = 0.1 * this.linkWidth;
		if (strokeWidth < (40.0 / scale)) strokeWidth = 40.0 / scale; // min pixel-width on screen
		if (strokeWidth > (100.0 / scale)) strokeWidth = 100.0 / scale;  // max pixel-width on screen
		display.setStroke(new BasicStroke((float)strokeWidth));

		for (DisplayableLinkI link : network.getLinks().values()) {
			if (!comp.checkLineInClip(link.getStartEasting(), link.getStartNorthing(),
					link.getEndEasting(), link.getEndNorthing())) {
				continue;
			}

			AffineTransform linkTransform = new AffineTransform(originalTransform);
			linkTransform.concatenate(boxTransform);
			linkTransform.concatenate(link.getLinear2PlaneTransform());

			/*
			 * (1) RENDER LINK
			 */

			display.setTransform(linkTransform);

			int cellLength_m = (int)Math.round(link.getLength_m() / link.getDisplayValueCount());
			int cellWidth_m = (int)Math.round(this.linkWidth);
			int cellStart_m = 0;

			for (int i = 0; i < link.getDisplayValueCount(); i++) {
				ArrayList<Double> times = this.stuckTimes.get(link.getId().toString());
				if (times != null) {
					if (this.mode == MODE_LINKS) {
						display.setColor(this.colorizerLinks.getColor(times.size()));
						display.fillRect(cellStart_m, -cellWidth_m * this.extraWidth, cellLength_m, cellWidth_m * this.extraWidth);
						display.setColor(Color.BLACK);
						display.drawRect(cellStart_m, -cellWidth_m * this.extraWidth, cellLength_m, cellWidth_m * this.extraWidth);
					} else if (this.mode == MODE_VEHICLES){
						double vehicleSize = link.getLength_m() / times.size();
						if (vehicleSize > 15) vehicleSize = 15;
						double position = cellLength_m - vehicleSize;
						display.setColor(Color.DARK_GRAY);
						display.drawRect(cellStart_m, -cellWidth_m, cellLength_m, cellWidth_m);
						for (Double time : times) {
							display.setColor(this.colorizerVehicles.getColor(time.doubleValue()));
							display.fill(new Rectangle2D.Double(position, -cellWidth_m, vehicleSize, cellWidth_m));
							position -= vehicleSize;
						}
					}
				} else {
					display.setColor(Color.DARK_GRAY);
					display.drawRect(cellStart_m, -cellWidth_m, cellLength_m, cellWidth_m);
				}

				cellStart_m += cellLength_m;
			}
		}

		display.setTransform(originalTransform);

	}

	public void setControlToolbar(final ControlToolbar toolbar) {
		this.toolbar = toolbar;
	}

	private class StuckEventCollector implements EventHandlerAgentStuckI {

		public void handleEvent(final EventAgentStuck event) {
			ArrayList<Double> times = LinkSetRendererStuck.this.stuckTimes.get(event.linkId);
			if (times == null) {
				times = new ArrayList<Double>(50);
			}
			times.add(event.time);
			LinkSetRendererStuck.this.stuckTimes.put(event.linkId, times);
		}

		public void reset(final int iteration) {
		}

	}

}
