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
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JScrollPane;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class NetVisScrollPane extends JScrollPane {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private final NetJComponent networkComponent;

	// -------------------- CONSTRUCTION --------------------

	NetVisScrollPane(final NetJComponent networkComponent) {
		super(networkComponent);
		this.networkComponent = networkComponent;
	}

	// -------------------- IMPLEMENTATION --------------------

	void updateViewClipRect() {
		final Dimension prefSize = this.networkComponent.getPreferredSize();
		final Rectangle rect = getViewport().getViewRect();
		double relX = (double) rect.getX() / prefSize.getWidth();
		double relY = (double) rect.getY() / prefSize.getHeight();
		this.networkComponent.setViewClipCoords(relX, relY, relX
				+ (double) rect.getWidth() / prefSize.getWidth(), relY
				+ (double) rect.getHeight() / prefSize.getHeight());
	}

	void moveNetwork(final int deltax, final int deltay) {
		final Rectangle rect = getViewport().getViewRect();
		rect.setLocation(rect.x + deltax, rect.y + deltay);
		this.getViewport().scrollRectToVisible(rect);
		this.getViewport().setViewPosition(rect.getLocation());
	}

	double scaleNetwork(final Rectangle destrect, double factor) {
		final Dimension prefSize = this.networkComponent.getPreferredSize();
		final Rectangle rect = getViewport().getViewRect();
		final double relX = (destrect.getX() + rect.getX())
				/ prefSize.getWidth();
		final double relY = (destrect.getY() + rect.getY())
				/ prefSize.getHeight();
		factor *= Math.min((double) rect.width / destrect.width,
				(double) rect.height / destrect.height);
		this.networkComponent.scale(factor);
		this.networkComponent.revalidate();
		final Dimension prefSize2 = this.networkComponent.getPreferredSize();
		this.networkComponent.setViewClipCoords(relX, relY, relX
				+ (double) rect.getWidth() / prefSize2.getWidth(), relY
				+ (double) rect.getHeight() / prefSize2.getHeight());
		final Point result = new Point();
		result.x = (int) (relX * prefSize2.getWidth());
		result.y = (int) (relY * prefSize2.getHeight());
		rect.setLocation(result.x, result.y);
		this.getViewport().scrollRectToVisible(rect);
		this.getViewport().setViewPosition(result);
		this.getViewport().toViewCoordinates(result);
		this.revalidate();
		this.repaint();
		return factor;
	}

	void scaleNetwork(final double factor) {
		final Dimension prefSize = this.networkComponent.getPreferredSize();
		final Rectangle rect = this.getViewport().getViewRect();
		final double relX = rect.getX() / prefSize.getWidth();
		final double relY = rect.getY() / prefSize.getHeight();
		this.networkComponent.scale(factor);
		this.networkComponent.revalidate();
		final Dimension prefSize2 = this.networkComponent.getPreferredSize();
		this.networkComponent.setViewClipCoords(relX, relY, relX
				+ (double) rect.getWidth() / prefSize2.getWidth(), relY
				+ (double) rect.getHeight() / prefSize2.getHeight());
		rect.x = (int) (relX * prefSize2.getWidth() + 0.5 * (rect.getWidth() * (prefSize2
				.getWidth()
				/ prefSize.getWidth() - 1.)));
		rect.y = (int) (relY * prefSize2.getHeight() + 0.5 * (rect.getHeight() * (prefSize2
				.getHeight()
				/ prefSize.getHeight() - 1.)));
		this.getViewport().setViewPosition(rect.getLocation());
		this.revalidate();
		this.repaint();
	}
}
