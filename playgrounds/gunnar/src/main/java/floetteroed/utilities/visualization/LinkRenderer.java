/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class LinkRenderer extends AbstractRenderer {

	// -------------------- CONSTANTS --------------------

	private final ValueColorizer colorizer;

	private final RenderableDynamicData<VisLink> data;

	// -------------------- CONSTRUCTION --------------------

	LinkRenderer(final VisConfig visConfig, final VisNetwork network,
			final RenderableDynamicData<VisLink> data) {
		super(visConfig, network);
		this.data = data;
		this.colorizer = new ValueColorizer(visConfig.getColorDef());
	}

	// -------------------- RENDERING --------------------

	@Override
	synchronized void myRendering(final Graphics2D display,
			final AffineTransform boxTransform) {
		this.drawLinks(true, display, boxTransform);
		this.drawLinks(false, display, boxTransform);
	}

	private void drawLinks(final boolean zeros, final Graphics2D display,
			final AffineTransform boxTransform) {

		final double laneWidth = 4.0 * getVisConfig().getLinkWidthFactor();
		final AffineTransform originalTransform = display.getTransform();
		display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));

		final NetJComponent comp = getComponent();

		for (VisLink link : this.getNetwork().getLinks()) {
			if (link.getVisible()) {

				final Double value = (this.data == null ? null : this.data
						.getCurrentValue(link));

				if ((value == null) || ((value != 0.0) && !zeros)
						|| ((value == 0.0) && zeros)) {

					/*
					 * (1) prepare the rendering
					 */
					final double startEasting = link.getFromNode().getEasting();
					final double startNorthing = link.getFromNode()
							.getNorthing();
					final double endEasting = link.getToNode().getEasting();
					final double endNorthing = link.getToNode().getNorthing();
					if (!comp.checkLineInClip(startEasting, startNorthing,
							endEasting, endNorthing)) {
						continue;
					}
					if (startEasting == endEasting
							&& startNorthing == endNorthing) {
						continue;
					}
					AffineTransform linkTransform = new AffineTransform(
							originalTransform);
					linkTransform.concatenate(boxTransform);
					linkTransform.concatenate(link.getTransform());
					/*
					 * (2) do the rendering
					 */
					display.setTransform(linkTransform);
					final int lanes;
					if (this.getVisConfig().getMultiLane()) {
						lanes = link.getLanes();
					} else {
						lanes = 1;
					}
					final int linkLength_m = (int) Math.round(link
							.getLength_m());
					final int cellWidth_m = (int) Math.round(laneWidth * lanes);
					int cellStart_m = 0;
					// if (data != null) {
					// final double value = this.data.getCurrentValue(link);
					// display.setColor(colorizer.getColor(value));
					// } else {
					// display.setColor(Color.WHITE);
					// }
					if (value != null) {
						display.setColor(colorizer.getColor(value));
					} else {
						display.setColor(Color.WHITE);
					}
					display.fillRect(cellStart_m, -cellWidth_m, linkLength_m,
							cellWidth_m);
					display.setColor(Color.BLACK);
					display.drawRect(cellStart_m, -cellWidth_m, linkLength_m,
							cellWidth_m);

				}

			}

		}

		display.setTransform(originalTransform);
	}

}
