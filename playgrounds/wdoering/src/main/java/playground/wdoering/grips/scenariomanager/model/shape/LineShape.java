package playground.wdoering.grips.scenariomanager.model.shape;

import java.awt.Point;
import java.awt.geom.Point2D;

public class LineShape extends Shape
{
	private Point2D c0;
	private Point2D c1;
	
	private Point pixelC0;
	private Point pixelC1;
	
	private int offsetX = 0;
	private int offsetY = 0;
	
	private boolean isArrow = false;
	
	public LineShape(Point2D c0, Point2D c1)
	{
		this.c0 = c0;
		this.c1 = c1;
		this.id = (++this.currentNumberId) + "_line";
	}
	
	public LineShape(int shapeRendererId, Point2D c0, Point2D c1)
	{
		this.c0 = c0;
		this.c1 = c1;
		this.id = (++this.currentNumberId) + "_line";
		this.layerID = shapeRendererId;
	}
	
	public void setArrow(boolean isArrow)
	{
		this.isArrow = isArrow;
	}
	
	public boolean isArrow()
	{
		return isArrow;
	}
	
	public void setOffsetX(int offsetX)
	{
		this.offsetX = offsetX;
	}
	
	public void setOffsetY(int offsetY)
	{
		this.offsetY = offsetY;
	}
	
	public int getOffsetX()
	{
		return offsetX;
	}
	
	public int getOffsetY()
	{
		return offsetY;
	}

	public Point getPixelC0()
	{
		return pixelC0;
	}
	public Point getPixelC1()
	{
		return pixelC1;
	}
	
	public void setPixelC0(Point pixelC0)
	{
		this.pixelC0 = pixelC0;
	}
	public void setPixelC1(Point pixelC1)
	{
		this.pixelC1 = pixelC1;
	}
	
	public Point2D getC0()
	{
		return c0;
	}
	
	public Point2D getC1()
	{
		return c1;
	}
	
	public void setC0(Point2D c0)
	{
		this.c0 = c0;
	}
	
	public void setC1(Point2D c1)
	{
		this.c1 = c1;
	}


}
