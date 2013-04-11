package playground.wdoering.grips.scenariomanager.model.imagecontainer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;


public interface ImageContainerInterface
{
	public <T> T getImage();
	public int getWidth();
	public int getHeight();
	public int getBorderWidth();
	public void drawBufferedImage(int i, int j, BufferedImage mapImage);
	
	
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
	
	
}
