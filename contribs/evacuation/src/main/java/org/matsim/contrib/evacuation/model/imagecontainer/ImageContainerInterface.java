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

package org.matsim.contrib.evacuation.model.imagecontainer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/**
 * image container interface.
 * delivers all functions for drawing
 * 
 * @author wdoering
 *
 */
public interface ImageContainerInterface
{
	public <T> T getImage();
	public int getWidth();
	public int getHeight();
	public int getBorderWidth();
	public void drawBufferedImage(int i, int j, BufferedImage mapImage);
	
	public <T> void setImage(T image);
	
	
	//paint methods
	public void setColor(Color color);
	public void setLineThickness(float thickness);
	
	public void drawLine(int x1, int y1, int x2, int y2);
	public void drawCircle(int x, int y, int width, int height);
	public void drawRect(int x, int y, int width, int height);
	public void drawPolygon(Polygon polygon);
	
	public void fillCircle(int x, int y, int width, int height);
	public void fillRect(int x, int y, int width, int height);
	public void fillPolygon(Polygon polygon);
	
	public void setFont(Font font);
	public void drawString(int x, int y, String string);
	
	public void translate(int x, int y);
	public void scale(double sx, double sy);
	public void drawLine(Point c0, Point c1);
	public void drawImage(String imageFile, int x, int y, int w, int h);
	public void drawImage(BufferedImage image, int x, int y, int w, int h);
	
	
}
