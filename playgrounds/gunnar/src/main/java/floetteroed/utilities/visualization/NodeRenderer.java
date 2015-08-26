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
class NodeRenderer extends AbstractRenderer {

	// -------------------- MEMBERS --------------------

	private boolean renderNodes = false;

	// -------------------- CONSTRUCTION --------------------

	NodeRenderer(final VisConfig visConfig, final VisNetwork network) {
		super(visConfig, network);
	}

	void setRenderNodes(final boolean renderNodes) {
		this.renderNodes = renderNodes;
	}

	// -------------------- RENDERING --------------------

	@Override
	synchronized void myRendering(final Graphics2D display,
			final AffineTransform boxTransform) {

		if (!this.renderNodes) {
			return;
		}

		final double laneWidth = 4.0 * getVisConfig().getLinkWidthFactor();
		final NetJComponent comp = getComponent();
		final AffineTransform originalTransform = display.getTransform();
		display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));

		AffineTransform nodeTransform = new AffineTransform(originalTransform);
		nodeTransform.concatenate(boxTransform);
		display.setTransform(nodeTransform);

		for (VisNode node : this.getNetwork().getNodes()) {

			final double x = node.getEasting();
			final double y = node.getNorthing();

			if (!comp.checkLineInClip(x, y, x, y)) {
				continue;
			}

			display.setColor(Color.WHITE);
			display.fillOval((int) (x - laneWidth), (int) (y - laneWidth),
					(int) (2.0 * laneWidth), (int) (2.0 * laneWidth));

			display.setColor(Color.BLACK);
			display.drawOval((int) (x - laneWidth), (int) (y - laneWidth),
					(int) (2.0 * laneWidth), (int) (2.0 * laneWidth));
		}

		display.setTransform(originalTransform);
	}
}
