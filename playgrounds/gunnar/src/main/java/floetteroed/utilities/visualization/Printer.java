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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JViewport;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class Printer implements Printable {

	// -------------------- CONSTANTS --------------------

	private final Component frame;

	private final JViewport viewPort;

	// -------------------- CONSTRUCTION --------------------

	Printer(final Component frame, final JViewport viewPort) {
		this.frame = frame;
		this.viewPort = viewPort;
	}

	// -------------------- IMPLEMENTATION --------------------

	void run() {
		final PrinterJob job = PrinterJob.getPrinterJob();
		job.setPrintable(this);
		final boolean ok = job.printDialog();
		if (ok) {
			try {
				job.print();
			} catch (PrinterException ex) {
				// job did not print for some reason
			}
		}
	}

	// -------------------- IMPLEMENTATION OF Printable --------------------

	public int print(final Graphics g, final PageFormat pf, final int page)
			throws PrinterException {

		if (page > 0) {
			return NO_SUCH_PAGE;
		}

		final Graphics2D g2d = (Graphics2D) g;
		final Rectangle viewRect = this.viewPort.getViewRect();
		final double scale = Math.min(pf.getWidth() / viewRect.width, pf
				.getHeight()
				/ viewRect.height);
		g2d.scale(scale, scale);
		g2d.translate(pf.getImageableX() - viewRect.x, pf.getImageableY()
				- viewRect.y);

		this.frame.printAll(g);
		return PAGE_EXISTS;
	}
}
