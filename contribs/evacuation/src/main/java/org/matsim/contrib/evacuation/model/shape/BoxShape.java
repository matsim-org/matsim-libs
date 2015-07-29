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

package org.matsim.contrib.evacuation.model.shape;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class BoxShape extends Shape
{
	private Rectangle2D box;
	private Rectangle pixelBox;
	private String imageFile;
	private boolean hasImage;
	private boolean fixedSize;
	private int fixedWidth;
	private int fixedHeight;
	private int offsetX;
	private int offsetY;
	private BufferedImage image;
	
	public BoxShape(int layerID, double x, double y, double w, double h)
	{
		this.layerID = layerID;
		this.box = new Rectangle2D.Double(x, y, w, h);
		this.id = (++Shape.currentNumberId) + "_box";
	}
	
	public int getOffsetX()
	{
		return offsetX;
	}
	
	public int getOffsetY()
	{
		return offsetY;
	}
	
	public void setOffsetX(int offsetX)
	{
		this.offsetX = offsetX;
	}
	
	public void setOffsetY(int offsetY)
	{
		this.offsetY = offsetY;
	}
	
	public BoxShape(int layerID, Point2D pos)
	{
		this.layerID = layerID;
		this.box = new Rectangle2D.Double(pos.getX(), pos.getY(), 0, 0);
		this.id = (++Shape.currentNumberId) + "_box";
	}
	
	public BoxShape(int layerID, Rectangle2D rectangle)
	{
		this.layerID = layerID;
		this.box = rectangle;
	}

	public boolean hasImage()
	{
		return hasImage;
	}
	
	public Rectangle2D getBox()
	{
		return box;
	}
	
	public boolean isFixedSize()
	{
		return fixedSize;
	}
	
	public void setFixedSize(int x, int y)
	{
		this.fixedWidth = x;
		this.fixedHeight = y;
		fixedSize = true;
	}
	
	
	
	public int getFixedHeight()
	{
		return fixedHeight;
	}
	
	public int getFixedWidth()
	{
		return fixedWidth;
	}
	
	public Rectangle getPixelBox()
	{
		return pixelBox;
	}
	
	public void setPixelBox(Rectangle pixelBox)
	{
		this.pixelBox = pixelBox;
	}
	
	public void setImageFile(String imageFile)
	{
		this.imageFile = imageFile;
		
		if ((this.imageFile!=null) && (this.imageFile != ""))
			hasImage = true;
	}
	
	public String getImageFile()
	{
		return imageFile;
	}

	public void setFixedSize(boolean b)
	{
		this.fixedSize = true;
		
	}

	public void setOffset(int i, int j)
	{
		this.offsetX = i;
		this.offsetY = j;
		
	}
	
	public BufferedImage getImage()
	{
		return image;
	}
	
	public void setImage(BufferedImage image)
	{
		this.image = image;
		
		if (this.image!=null)
			hasImage = true;
	}

	public void setImage(Image image)
	{
		this.setImage((BufferedImage)image);
	}


}
