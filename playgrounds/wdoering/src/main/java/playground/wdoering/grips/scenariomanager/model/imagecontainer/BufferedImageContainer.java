package playground.wdoering.grips.scenariomanager.model.imagecontainer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;


public class BufferedImageContainer implements ImageContainerInterface
{
	protected BufferedImage image;
	protected Graphics2D imageGraphics;
	protected int borderWidth;
	protected ConcurrentHashMap<String, BufferedImage> images;
	
	public BufferedImageContainer(BufferedImage image, int border)
	{
		this.image = image;
		this.imageGraphics = (Graphics2D)this.image.getGraphics(); 
		this.borderWidth = border;
		
		this.images = new ConcurrentHashMap<String, BufferedImage>();
	}

	@Override
	public int getWidth()
	{
		return image.getWidth();
	}

	@Override
	public int getHeight()
	{
		return image.getHeight();
	}
	
	@Override
	public void drawBufferedImage(int x, int y, BufferedImage image)
	{
		this.imageGraphics.drawImage(image, x, y, null);
	}
	
	public Graphics2D getImageGraphics()
	{
		return imageGraphics;
	}
	
	@Override
	public int getBorderWidth()
	{
		return borderWidth;
	}

	@Override
	public void setColor(Color color)
	{
		this.imageGraphics.setColor(color);
	}

	@Override
	public void setLineThickness(float thickness)
	{
		this.imageGraphics.setStroke(new BasicStroke(thickness));		
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2)
	{
		this.imageGraphics.drawLine(x1, y1, x2, y2);
	}


	@Override
	public void drawRect(int x, int y, int width, int height)
	{
		this.imageGraphics.drawRect(x, y, width, height);
	}

	@Override
	public void drawPolygon(Polygon polygon)
	{
		this.imageGraphics.drawPolygon(polygon);
	}


	@Override
	public void fillRect(int x, int y, int width, int height)
	{
		this.imageGraphics.fillRect(x, y, width, height);
	}

	@Override
	public void fillPolygon(Polygon polygon)
	{
		this.imageGraphics.fillPolygon(polygon);
	}

	@Override
	public void setFont(Font font)
	{
		this.imageGraphics.setFont(font);
	}

	@Override
	public void drawString(int x, int y, String string)
	{
		this.imageGraphics.drawString(string,x,y);
	}

	@SuppressWarnings("unchecked")
	@Override
	public BufferedImage getImage()
	{
		return this.image;
	}

	@Override
	public void drawCircle(int x, int y, int width, int height)
	{
		this.imageGraphics.drawOval(x,y,width,height);		
	}

	@Override
	public void fillCircle(int x, int y, int width, int height)
	{
		this.imageGraphics.fillOval(x,y,width,height);		
		
	}
	
	@Override
	public void translate(int x, int y)
	{
		this.imageGraphics.translate(x, y);
	}
	
	@Override
	public void scale(double sx, double sy)
	{
		this.imageGraphics.scale(sx, sy);
		
	}
	
	@Override
	public void drawLine(Point c0, Point c1)
	{
		this.imageGraphics.drawLine(c0.x, c0.y, c1.x, c1.y);
		
	}
	
	@Override
	public void drawImage(String imageFile, int x, int y, int w, int h)
	{
		if (!this.images.contains(imageFile))
		{
			try
			{
				BufferedImage newImage = ImageIO.read(new File(imageFile));
				this.images.put(imageFile, newImage);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
			
		this.imageGraphics.drawImage(this.images.get(imageFile), x, y, w, h, null);
		
	}
	

}
