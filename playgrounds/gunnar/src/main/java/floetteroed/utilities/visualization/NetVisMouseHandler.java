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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class NetVisMouseHandler extends MouseInputAdapter implements ChangeListener {

	// -------------------- CONSTANTS --------------------

	private final NetVis netVis;

	// -------------------- MEMBERS --------------------

	private Point start = null;

	private Rectangle currentRect = null;

	private int button = 0;

	// -------------------- CONSTRUCTION --------------------

	NetVisMouseHandler(final NetVis netVis) {
		this.netVis = netVis;
	}

	// --------------------OVERRIDING OF MouseInputAdapter --------------------

	void updateSize(final MouseEvent e) {
		this.currentRect = new Rectangle(this.start);
		this.currentRect.add(e.getX(), e.getY());
		this.netVis.networkScrollPane().getGraphics().drawRect(
				this.currentRect.x, this.currentRect.y, this.currentRect.width,
				this.currentRect.height);
		this.netVis.networkScrollPane().repaint();
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		this.button = e.getButton();
		this.start = new Point(x, y);
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (this.button == MouseEvent.BUTTON1) {
			this.updateSize(e);
		} else if (this.button == MouseEvent.BUTTON2) {
			int deltax = this.start.x - e.getX();
			int deltay = this.start.y - e.getY();
			this.start.x = e.getX();
			this.start.y = e.getY();
			this.netVis.networkScrollPane().moveNetwork(deltax, deltay);
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		if (this.button == MouseEvent.BUTTON1) {
			updateSize(e);
			if ((this.currentRect.getHeight() > 10)
					&& (this.currentRect.getWidth() > 10)) {
				double scale = this.netVis.buttonComponent().getScale();
				scale = this.netVis.networkScrollPane().scaleNetwork(
						this.currentRect, scale);
				this.netVis.buttonComponent().setScale(scale);
			}
			this.currentRect = null;
		}
		this.button = 0;
	}

	// --------------- IMPLEMENTATION OF ChangeListener ---------------

	@Override
	public void stateChanged(ChangeEvent e) {
		this.netVis.networkScrollPane().updateViewClipRect();
	}
}
