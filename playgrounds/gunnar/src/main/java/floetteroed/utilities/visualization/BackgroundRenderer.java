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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class BackgroundRenderer extends AbstractRenderer {

	// -------------------- CONSTRUCTION --------------------

	BackgroundRenderer(final VisConfig visConfig, final VisNetwork net) {
		super(visConfig, net);
	}

	// -------------------- RENDERING --------------------

	@Override
	void myRendering(Graphics2D display, AffineTransform boxTransform) {

		Rectangle visRect = getComponent().getVisibleRect();

		display.setBackground(Color.WHITE);
		display.clearRect(visRect.x, visRect.y, visRect.width, visRect.height);

		final String logo = getVisConfig().getLogo();
		if (logo != null && logo.length() > 0) {

			int targetLogoHeight = visRect.height * 8 / 10;
			int targetLogoWidth = visRect.width * 9 / 10;

			display.setFont(new Font(display.getFont().getName(), Font.BOLD,
					targetLogoHeight));
			int trueLogoWidth = display.getFontMetrics().stringWidth(logo);

			double scaling = Math.min((double) targetLogoWidth / trueLogoWidth,
					1);

			int scaledLogoHeight = (int) Math.round(scaling * targetLogoHeight);
			display.setFont(new Font(display.getFont().getName(), Font.PLAIN,
					scaledLogoHeight));
			int scaledLogoWidth = display.getFontMetrics().stringWidth(logo);

			display.setColor(Color.LIGHT_GRAY);

			int xCoord = visRect.x + (visRect.width - scaledLogoWidth) / 2;
			int yCoord = visRect.y + (visRect.height + scaledLogoHeight) / 2;

			display.drawString(logo, xCoord, yCoord);
		}
	}
}
