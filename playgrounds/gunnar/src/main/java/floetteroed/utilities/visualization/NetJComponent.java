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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class NetJComponent extends JComponent {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private static final double BORDER_FACTOR = 0.0;

	private final AbstractRenderer networkRenderer;

	private final VisConfig visconfig;

	private final int frameDefaultWidth;

	private final int frameDefaultHeight;

	private double viewMinX, viewMinY, viewMaxX, viewMaxY;

	// -------------------- MEMBERS --------------------

	// -------------------- CONSTRUCTION --------------------

	NetJComponent(final AbstractRenderer networkRenderer,
			final VisConfig visconfig) {
		this.networkRenderer = networkRenderer;
		this.visconfig = visconfig;

		// calculate size of frame
		final Dimension screenSize = Toolkit.getDefaultToolkit()
				.getScreenSize();
		double factor = (double) screenSize.getWidth() / networkClippingWidth();
		factor = (double) Math.min(factor, screenSize.getHeight()
				/ networkClippingHeight());
		factor *= 0.8;
		this.frameDefaultWidth = (int) Math.floor(networkClippingWidth()
				* factor);
		this.frameDefaultHeight = (int) Math.floor(networkClippingHeight()
				* factor);

		this.scale(1.0);
		this.setViewClipCoords(0, 0, 1, 1);
	}

	// -------------------- IMPLEMENTATION --------------------

	void setViewClipCoords(final double minX, final double minY,
			final double maxX, final double maxY) {
		this.viewMinX = this.networkClippingMinEasting() + minX
				* this.networkClippingWidth();
		this.viewMaxX = this.networkClippingMinEasting() + maxX
				* this.networkClippingWidth();
		this.viewMinY = this.networkClippingMinNorthing() + (1.0 - maxY)
				* this.networkClippingHeight();
		this.viewMaxY = this.networkClippingMinNorthing() + (1.0 - minY)
				* this.networkClippingHeight();
	}

	void moveViewClipCoords(final double deltaX, final double deltaY) {
		this.viewMinX += deltaX * this.networkClippingWidth();
		this.viewMaxX += deltaX * this.networkClippingWidth();
		this.viewMinY -= deltaY * this.networkClippingHeight();
		this.viewMaxY -= deltaY * this.networkClippingHeight();
	}

	/*-
	 * returns something like this
	 * 0 1 2
	 * 4 5 6
	 * 8 9 10
	 * where 5 means the cord is IN the clipping region
	 */
	int checkViewClip(final double x, final double y) {
		final int xquart = x < viewMinX ? 0 : x > viewMaxX ? 2 : 1;
		final int yquart = y < viewMinY ? 0 : y > viewMaxY ? 2 : 1;
		return xquart + 4 * yquart;
	}

	boolean checkLineInClip(final double sx, final double sy, final double ex,
			final double ey) {
		final int qstart = checkViewClip(sx, sy);
		final int qend = checkViewClip(ex, ey);
		// both in same sector, that is not middle sector
		if ((qstart == qend) && qstart != 5) {
			return false;
		}
		// both are either left or right and not in the middle
		if ((qstart % 4) == (qend % 4) && (qstart % 4) != 1) {
			return false;
		}
		// both are either top or bottom but not in the middle
		if ((qstart / 4) == (qend / 4) && (qstart / 4) != 1) {
			return false;
		}
		return true; // all other cases are possibly visible
	}

	void scale(final double factor) {
		if (factor > 0) {
			final int scaledWidth = (int) Math.round(factor
					* this.frameDefaultWidth);
			final int scaledHeight = (int) Math.round(factor
					* this.frameDefaultHeight);
			this.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
		}
	}

	// -------------------- COORDINATE TRANSFORMATION --------------------

	// computes some fixed border around linear networks
	private double additionalBorder() {
		return 0.1 * Math.max(this.networkRenderer.getNetwork().getMaxEasting()
				- this.networkRenderer.getNetwork().getMinEasting(),
				this.networkRenderer.getNetwork().getMaxNorthing()
						- this.networkRenderer.getNetwork().getMinNorthing());
	}

	private double networkClippingEastingBorder() {
		return Math
				.max(1,
						BORDER_FACTOR
								* (this.networkRenderer.getNetwork()
										.getMaxEasting() - this.networkRenderer
										.getNetwork().getMinEasting()))
				+ additionalBorder();
	}

	private double networkClippingNorthingBorder() {
		return Math
				.max(1,
						BORDER_FACTOR
								* (this.networkRenderer.getNetwork()
										.getMaxNorthing() - this.networkRenderer
										.getNetwork().getMinNorthing()))
				+ additionalBorder();
	}

	private double networkClippingMinEasting() {
		return this.networkRenderer.getNetwork().getMinEasting()
				- this.networkClippingEastingBorder();
	}

	private double networkClippingMaxEasting() {
		return this.networkRenderer.getNetwork().getMaxEasting()
				+ this.networkClippingEastingBorder();
	}

	private double networkClippingMinNorthing() {
		return this.networkRenderer.getNetwork().getMinNorthing()
				- this.networkClippingNorthingBorder();
	}

	private double networkClippingMaxNorthing() {
		return this.networkRenderer.getNetwork().getMaxNorthing()
				+ this.networkClippingNorthingBorder();
	}

	private double networkClippingWidth() {
		return this.networkClippingMaxEasting()
				- this.networkClippingMinEasting();
	}

	private double networkClippingHeight() {
		return this.networkClippingMaxNorthing()
				- this.networkClippingMinNorthing();
	}

	private AffineTransform getBoxTransform() {

		// two original extreme coordinates ...

		final double v1 = networkClippingMinEasting();
		final double w1 = networkClippingMinNorthing();

		final double v2 = networkClippingMaxEasting();
		final double w2 = networkClippingMaxNorthing();

		// ... mapped onto two extreme picture coordinates ...

		final Dimension prefSize = this.getPreferredSize();

		final double x1 = 0;
		final double y1 = (int) prefSize.getHeight();

		final double x2 = (int) prefSize.getWidth();
		final double y2 = 0;

		// ... yields a simple affine transformation without shearing:

		final double m00 = (x1 - x2) / (v1 - v2);
		final double m02 = x1 - m00 * v1;

		final double m11 = (y1 - y2) / (w1 - w2);
		final double m12 = y1 - m11 * w1;

		return new AffineTransform(m00, 0.0, 0.0, m11, m02, m12);
	}

	Point2D.Double getNetCoord(final double x, final double y) {
		final Point2D.Double result = new Point2D.Double();
		final Dimension prefSize = getPreferredSize();
		result.x = x / prefSize.width;
		result.y = 1. - y / prefSize.height;
		result.x *= this.networkRenderer.getNetwork().getMaxEasting()
				- this.networkRenderer.getNetwork().getMinEasting();
		result.y *= this.networkRenderer.getNetwork().getMaxNorthing()
				- this.networkRenderer.getNetwork().getMinNorthing();
		result.x += this.networkRenderer.getNetwork().getMinEasting();
		result.y += this.networkRenderer.getNetwork().getMinNorthing();
		return result;
	}

	// -------------------- OVERRIDING OF JComponent --------------------

	public void paint(final Graphics g) {
		final Graphics2D g2 = (Graphics2D) g;
		if (this.visconfig.getUseAntiAliasing()) {
			g2.addRenderingHints(new RenderingHints(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON));
		} else {
			g2.addRenderingHints(new RenderingHints(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF));
		}
		this.networkRenderer.render(g2, getBoxTransform());
	}

}
