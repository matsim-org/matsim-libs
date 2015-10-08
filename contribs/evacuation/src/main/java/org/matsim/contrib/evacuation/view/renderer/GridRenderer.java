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

package org.matsim.contrib.evacuation.view.renderer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.evacuation.analysis.EAToolBox;
import org.matsim.contrib.evacuation.analysis.data.AttributeData;
import org.matsim.contrib.evacuation.analysis.data.Cell;
import org.matsim.contrib.evacuation.analysis.data.EventData;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.Constants.Mode;
import org.matsim.contrib.evacuation.model.Constants.Unit;
import org.matsim.contrib.evacuation.model.config.ToolConfig;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class GridRenderer extends AbstractRenderLayer {

	private Mode mode = Mode.EVACUATION;
	private float transparency;
	private QuadTree<Cell> cellTree;

	private Cell selectedCell;
	private CoordinateTransformation ctInverse;
	private EventData data;
	private ArrayList<Link> links;

	public GridRenderer(Controller controller) {
		super(controller);

		this.ctInverse = this.controller.getCtTarget2Osm();
	}

	public float getTransparency() {
		return transparency;
	}

	public void setTransparency(float transparency) {
		this.transparency = transparency;
	}

	@Override
	public synchronized void paintLayer() {
		data = this.controller.getEventData();
		links = this.controller.getLinkList();

		if (data == null)
			return;
		else
			this.cellTree = data.getCellTree();

		// viewport
		this.imageContainer.translate(-controller.getViewportBounds().x,
				-controller.getViewportBounds().y);

		// draw the grid
		drawGrid(mode, true);

		// draw utilization
		if (mode.equals(Mode.UTILIZATION))
			drawUtilization();

		// viewport
		this.imageContainer.translate(controller.getViewportBounds().x,
				controller.getViewportBounds().y);
	}

	private void drawUtilization() {
		if ((links == null) || (links.size() == 0))
			return;

		HashMap<Id<Link>, List<Tuple<Id<Person>, Double>>> linkLeaveTimes = data
				.getLinkLeaveTimes();
		HashMap<Id<Link>, List<Tuple<Id<Person>, Double>>> linkEnterTimes = data
				.getLinkEnterTimes();

		for (Link link : this.links) {
			List<Tuple<Id<Person>, Double>> leaveTimes = linkLeaveTimes.get(link
					.getId());
			List<Tuple<Id<Person>, Double>> enterTimes = linkEnterTimes.get(link
					.getId());

			if ((enterTimes != null) && (enterTimes.size() > 0)
					&& (leaveTimes != null)) {

				Coord fromCoord = this.controller.getCtTarget2Osm().transform(
						new Coord(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()));
				Point2D fromP2D = this.controller
						.geoToPixel(new Point2D.Double(fromCoord.getY(),
								fromCoord.getX()));

				Coord toCoord = this.controller.getCtTarget2Osm().transform(
						new Coord(link.getToNode().getCoord().getX(), link
								.getToNode().getCoord().getY()));
				Point2D toP2D = this.controller.geoToPixel(new Point2D.Double(
						toCoord.getY(), toCoord.getX()));

				float strokeWidth = 1;
				Color linkColor = Color.BLUE;

				if (data.getLinkUtilizationVisData() != null) {
					if (data.getLinkUtilizationVisData().getAttribute(
							link.getId()) != null) {
						Tuple<Float, Color> currentColoration = (Tuple<Float, Color>) data
								.getLinkUtilizationVisData().getAttribute(
										link.getId());
						strokeWidth = ((currentColoration.getFirst() * 35f) / (float) Math
								.pow(2, this.controller.getZoom()));
						linkColor = currentColoration.getSecond();
					}
				}

				this.imageContainer.setLineThickness(strokeWidth);

				this.imageContainer.setColor(linkColor);
				// g.setColor(Color.RED);
				this.imageContainer.drawLine((int) fromP2D.getX(),
						(int) fromP2D.getY(), (int) toP2D.getX(),
						(int) toP2D.getY());
			}

		}
	}

	/**
	 * draw the grid
	 * 
	 * @param drawToolTip
	 * 
	 */
	private void drawGrid(Mode mode, boolean drawToolTip) {
		Point currentMousePosition = this.controller.getMousePosition();
		double gridSize = this.data.getCellSize();

		if (this.data != null) {
			this.imageContainer.setColor(Color.BLACK);
			this.imageContainer.setLineThickness(1);

			// get all cells from celltree
			LinkedList<Cell> cells = new LinkedList<Cell>();
			cellTree.getRectangle(new Rect(Double.NEGATIVE_INFINITY,
					Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
					Double.POSITIVE_INFINITY), cells);

			this.selectedCell = null;

			for (Cell cell : cells) {

				// get cell coordinate (+ gridsize) and transform into pixel
				// coordinates
				Coord cellCoord = cell.getCoord();
				Coord transformedCoord = this.ctInverse
						.transform(new Coord(cellCoord.getX() - gridSize
								/ 2, cellCoord.getY() - gridSize / 2));
				Point2D cellCoordP2D = this.controller
						.geoToPixel(new Point2D.Double(transformedCoord.getY(),
								transformedCoord.getX()));
				Coord cellPlusGridCoord = this.ctInverse
						.transform(new Coord(cellCoord.getX() + gridSize
								/ 2, cellCoord.getY() + gridSize / 2));
				Point2D cellPlusGridCoordP2D = this.controller
						.geoToPixel(new Point2D.Double(
								cellPlusGridCoord.getY(), cellPlusGridCoord
										.getX()));

				// adjust coordinates using the viewport
				int gridX1 = (int) cellCoordP2D.getX();
				int gridY1 = (int) cellCoordP2D.getY();
				int gridX2 = (int) cellPlusGridCoordP2D.getX();
				int gridY2 = (int) cellPlusGridCoordP2D.getY();

				// make sure the first values are the smaller ones, if not: swap
				if (gridX1 > gridX2) {
					int temp = gridX2;
					gridX2 = gridX1;
					gridX1 = temp;
				}
				if (gridY1 > gridY2) {
					int temp = gridY2;
					gridY2 = gridY1;
					gridY1 = temp;
				}

				this.imageContainer.setLineThickness(1);

				// color grid (if mode equals evacuation or clearing time)
				if ((mode.equals(Mode.EVACUATION))
						|| (mode.equals(Mode.CLEARING))) {
					AttributeData<Color> visData;

					// colorize cell depending on the picked colorization, cell
					// data and the relative travel or clearance time
					this.imageContainer
							.setColor(ToolConfig.COLOR_DISABLED_TRANSPARENT); // default

					if (mode.equals(Mode.EVACUATION)) {
						visData = data
								.getEvacuationTimeVisData();
						if ((cell.getCount() > 0))
							this.imageContainer.setColor(visData
									.getAttribute(cell.getId()));

					} else if (mode.equals(Mode.CLEARING)) {
						visData = data
								.getClearingTimeVisData();
						if (cell.getClearingTime() > 0)
							this.imageContainer.setColor(visData
									.getAttribute(cell.getId()));
					}

					this.imageContainer.fillRect(gridX1, gridY1, gridX2
							- gridX1, gridY2 - gridY1);
				}

				// draw grid
				if (mode.equals(Mode.UTILIZATION)) {
					this.imageContainer
							.setColor(ToolConfig.COLOR_GRID_UTILIZATION);
					this.imageContainer.setLineThickness(2);
					this.imageContainer.drawRect(gridX1, gridY1, gridX2
							- gridX1, gridY2 - gridY1);
					this.imageContainer.setColor(ToolConfig.COLOR_CELL);
					this.imageContainer.fillRect(gridX1, gridY1, gridX2
							- gridX1, gridY2 - gridY1);
				} else {
					this.imageContainer.setColor(ToolConfig.COLOR_GRID);
					this.imageContainer.setLineThickness(1);
					this.imageContainer.drawRect(gridX1, gridY1, gridX2
							- gridX1, gridY2 - gridY1);

				}

				if (drawToolTip && currentMousePosition != null) {
					int mouseX = this.controller.getMousePosition().x;
					int mouseY = this.controller.getMousePosition().y;

					if ((mouseX >= gridX1) && (mouseX < gridX2)
							&& (mouseY >= gridY1) && (mouseY < gridY2)) {
						this.imageContainer.setColor(ToolConfig.COLOR_HOVER);
						this.imageContainer.fillRect(gridX1, gridY1, gridX2
								- gridX1, gridY2 - gridY1);
						this.imageContainer.setLineThickness(3);
						this.imageContainer.drawRect(gridX1, gridY1, gridX2
								- gridX1, gridY2 - gridY1);

						this.selectedCell = cell;
					}

				}
			}

			// draw tooltip
			if ((this.selectedCell != null)
					&& (this.controller.getMousePosition() != null)) {
				Point mp = this.controller.getMousePosition();

				this.imageContainer.setLineThickness(1);
				this.imageContainer.setColor(new Color(0, 0, 0, 90));
				this.imageContainer.fillRect(
						this.controller.getMousePosition().x - 15, mp.y + 30,
						260, 85);
				this.imageContainer.setColor(Color.white);
				this.imageContainer.fillRect(mp.x - 25, mp.y + 20, 260, 85);
				this.imageContainer.setColor(Color.black);
				this.imageContainer.drawRect(mp.x - 25, mp.y + 20, 260, 85);

				this.imageContainer.setFont(ToolConfig.FONT_DEFAULT_BOLD);
				this.imageContainer.drawString(mp.x - 15, mp.y + 40,
						"person count:");
				this.imageContainer.drawString(mp.x - 15, mp.y + 60,
						"clearing time:");
				this.imageContainer.drawString(mp.x - 15, mp.y + 80,
						"average evacuation time:");

				this.imageContainer.setFont(ToolConfig.FONT_DEFAULT);
				this.imageContainer.drawString(mp.x + 135, mp.y + 40, EAToolBox
						.getReadableTime(selectedCell.getCount(), Unit.PEOPLE));
				this.imageContainer.drawString(
						mp.x + 135,
						mp.y + 60,
						EAToolBox.getReadableTime(
								selectedCell.getClearingTime(), Unit.TIME));
				this.imageContainer.drawString(
						mp.x + 135,
						mp.y + 80,
						EAToolBox.getReadableTime(selectedCell.getTimeSum()
								/ selectedCell.getCount(), Unit.TIME));

			}
			this.imageContainer.setColor(Color.black);
		}
	}

	public void setMode(Mode mode) {
		this.mode = mode;
		this.controller.paintLayers();

	}

	public void updateEventData(EventData data) {

	}

	public void setBounds(int x, int y, int width, int height) {

	}

}
