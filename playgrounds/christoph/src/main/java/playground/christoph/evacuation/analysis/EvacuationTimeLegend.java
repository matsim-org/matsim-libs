/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTimeLegend.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class EvacuationTimeLegend {
	
	private final int leftMargin = 10;
	private final int barWidth = 50;
	private final int barHeight = 15;
	private final int barSpacer = 8;
	private final int textWidth = 140;
	private final int textSpacer = 10;
	private final int textYShift = 3;
	private final int headerLineHeight = 15;
	private final int headerHeight = 40;
	private final int backgroundColor = makeARGB(200, 255, 255, 255);

	private final Color headerColor = Color.BLACK;
	private final Font headerFont = new Font("Arial", Font.BOLD, 14);
	private final Color textColor = Color.BLACK;
	private final Font textFont = new Font("Arial", Font.PLAIN, 12);
	
	/*package*/ BufferedImage createLegend(String header, String[] texts) {

		int width = barWidth + textWidth;
		int height = headerHeight + texts.length * barHeight + (texts.length - 2) * textSpacer;
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		
		setBackgroundColor(image, width, height);
		addHeader(image, header);
		
		int x = leftMargin;
		int y = headerHeight;
		
		for (int i = 0; i < texts.length; i++) {
			byte[] color = EvacuationTimePictureWriter.colorScale[i];
			String text = texts[i]; 
				
			/*
			 * Conversion is necessary here. Google uses:
			 * - ABGR instead of ARGB
			 * - a byte value range (-128 to 127) instead of 0 to 255
			 */
			int col = makeARGB(color[0] + 256, color[3] + 256, color[2] + 256, color[1] + 256);
			setColor(image, x, y, barWidth, barHeight, col);
			
			// add Text
			addText(image, text, x + barWidth + textSpacer, y + barHeight - textYShift);
			
			y = y + barHeight + barSpacer;
		}
		
		return image;
	}
	
	private void setBackgroundColor(BufferedImage image, int width, int height) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, backgroundColor);
			}
		}
	}
	
	private void addHeader(BufferedImage image, String header) {
		Graphics2D g = image.createGraphics();
		g.setColor(headerColor);
		g.setFont(headerFont);
		
		String[] headerLines = header.split("\n");
		
		int y = headerLineHeight;
		for (String headerLine : headerLines) {
			g.drawString(headerLine, leftMargin, y);
			y = y + headerLineHeight;
		}
	}
	
	private void addText(BufferedImage image, String text, int x, int y) {
		Graphics2D g = image.createGraphics();
		g.setColor(textColor);
		g.setFont(textFont);
		g.drawString(text, x, y);
	}
	
	private void setColor(BufferedImage image, int x, int y, int width, int height, int color) {
		for (int i = x; i < x + width; i++) {
			for (int j = y; j < y + height; j++) {
				image.setRGB(i, j, color);
			}
		}
	}

	/*
	 * RGB Encoding is 0xAARRGGBB 
	 * The following code will return an int value usable by setRGB(), given alpha, red, green, and blue values:
	 */
	private int makeARGB(int a, int r, int g, int b) {
		return a << 24 | r << 16 | g << 8 | b;
	}
}
