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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class LabelRenderer extends AbstractRenderer {

	// -------------------- CONSTANTS --------------------

	private final VisNetwork network;

	private final int textHeight = 12;

	// -------------------- CONSTRUCTION --------------------

	LabelRenderer(final VisConfig visConfig, final VisNetwork network) {
		super(visConfig, network);
		this.network = network;
	}

	// -------------------- RENDERING --------------------

	void myRendering(final Graphics2D display,
			final AffineTransform boxTransform) {

		final AffineTransform originalTransform = display.getTransform();
		final NetJComponent comp = getComponent();

		/*
		 * (1) node labels
		 */
		if (getVisConfig().getShowNodeLabels()) {
			for (VisNode node : this.network.getNodes()) {
				final double x = node.getEasting();
				final double y = node.getNorthing();
				if (comp.checkViewClip(x, y) != 5) {
					continue;
				}
				Point2D point = new Point2D.Double(x, y);
				AffineTransform nodeTransform = new AffineTransform(
						boxTransform);
				nodeTransform.transform(point, point);
				nodeTransform = new AffineTransform(originalTransform);
				nodeTransform.translate(point.getX(), point.getY());
				display.setTransform(nodeTransform);
				display.setFont(new Font(display.getFont().getName(),
						Font.PLAIN, textHeight));
				display.setColor(Color.BLACK);
				final String label = node.getId().toString();
				if (label != null && !"".equals(label)) {
					display.drawString(label, 0, 0);
				}
			}
		}
		display.setTransform(originalTransform);

		/*
		 * (2) link labels
		 */
		if (getVisConfig().getShowLinkLabels()) {
			for (VisLink link : this.network.getLinks()) {
				final double startEasting = link.getFromNode().getEasting();
				final double startNorthing = link.getFromNode().getNorthing();
				final double endEasting = link.getToNode().getEasting();
				final double endNorthing = link.getToNode().getNorthing();
				double xpos = startEasting + (endEasting - startEasting) * .42;
				double ypos = startNorthing + (endNorthing - startNorthing)
						* .42;
				if (comp.checkViewClip(xpos, ypos) != 5) {
					continue;
				}
				final Point2D point = new Point2D.Double(xpos, ypos);
				final AffineTransform linkTransform = new AffineTransform(
						boxTransform);
				linkTransform.transform(point, point);
				final AffineTransform linkTransform2 = new AffineTransform(
						originalTransform);
				linkTransform2.translate(point.getX(), point.getY());
				double dx = endEasting - startEasting;
				double dy = endNorthing - startNorthing;
				double theta = Math.atan2(dx, dy);
				if (theta <= 0) {
					linkTransform2.rotate(theta + Math.PI / 2.);
				} else {
					linkTransform2.rotate(theta - Math.PI / 2.);
				}
				display.setTransform(linkTransform2);
				display.setFont(new Font(display.getFont().getName(),
						Font.PLAIN, textHeight));
				display.setColor(Color.BLACK);
				final String label = link.getId().toString();
				if (label != null && !"".equals(label)) {
					final int textWidth = display.getFontMetrics().stringWidth(
							label);
					final int yoffset = (theta <= 0) ? (int) -textHeight / 2
							: (int) textHeight;
					display.drawString(label, (int) -textWidth / 2, yoffset);
				}
			}
		}

		display.setTransform(originalTransform);
	}
}
