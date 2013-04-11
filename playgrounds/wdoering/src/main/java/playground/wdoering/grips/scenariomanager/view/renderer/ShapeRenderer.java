package playground.wdoering.grips.scenariomanager.view.renderer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.ImageContainerInterface;
import playground.wdoering.grips.scenariomanager.model.shape.BoxShape;
import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.LineShape;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape.DrawMode;

public class ShapeRenderer extends AbstractRenderLayer
{
//	Random random = new Random();

	public ShapeRenderer(Controller controller, ImageContainerInterface imageContainer)
	{
		super(controller);
	}
	
	@Override
	public synchronized void paintLayer()
	{
		if (!enabled)
			return;
		
		ArrayList<Shape> shapes = controller.getActiveShapes();
		
		if (shapes!=null)
		{
			this.imageContainer.translate(-controller.getViewportBounds().x, -controller.getViewportBounds().y);
			
			for (Shape shape : shapes)
			{
				if ((!shape.isVisible()) || (shape.getLayerID()!=this.id))
					continue;
				
				if (shape.isSelected())
					this.imageContainer.setColor(shape.getStyle().getSelectColor());
				else
					this.imageContainer.setColor(shape.getColor());
				
				this.imageContainer.setLineThickness(shape.getThickness());
				
				if (shape instanceof CircleShape)
				{
					int x = ((CircleShape)shape).getPixelOrigin().x;
					int y = ((CircleShape)shape).getPixelOrigin().y;
					int radius = ((CircleShape)shape).getPixelRadius();
					
					//fill shape (if style is not just to draw the contour)
					if (!shape.getDrawMode().equals(DrawMode.CONTOUR))
						this.imageContainer.fillCircle(x-radius/2,y-radius/2,radius,radius);
					
					//draw contour
					if (!shape.getDrawMode().equals(DrawMode.FILL))
					{
						this.imageContainer.setColor(shape.getContourColor());
						this.imageContainer.drawCircle(x-radius/2,y-radius/2,radius,radius);
					}
				}
				else if (shape instanceof BoxShape)
				{
					int x = ((BoxShape)shape).getPixelBox().x;
					int y = ((BoxShape)shape).getPixelBox().y;
					int w = ((BoxShape)shape).getPixelBox().width;
					int h = ((BoxShape)shape).getPixelBox().height;
					
					if (((BoxShape)shape).hasImage())
						this.imageContainer.drawImage(((BoxShape)shape).getImageFile(),x,y,w,h);
					else
					{
	//					System.out.println("boxpixcoord:" + ((BoxShape)shape).getPixelBox());
						
						//draw filled rectangle
						if (!shape.getDrawMode().equals(DrawMode.CONTOUR))
							this.imageContainer.fillRect(x,y,w,h);
						
						//draw contour of rectangle
						if (!shape.getDrawMode().equals(DrawMode.FILL))
						{
							this.imageContainer.setColor(shape.getContourColor());
							this.imageContainer.drawRect(x,y,w,h);
						}
					}
				}
				else if (shape instanceof PolygonShape)
				{
					java.awt.Polygon pixelPolygon = ((PolygonShape)shape).getPixelPolygon();
					
//					if (pixelPolygon.contains(this.controller.getMousePosition()))
//						this.imageContainer.setColor(shape.getStyle().getHoverColor());
					
					if (shape.isHover())
						this.imageContainer.setColor(shape.getStyle().getHoverColor());
					
//						shape.setHover(true);
//					else
//						shape.setHover(false);
					
//					System.out.println(((PolygonShape)shape).getPolygon().getEnvelope());
					
					if (!shape.getDrawMode().equals(DrawMode.CONTOUR))
						this.imageContainer.fillPolygon(pixelPolygon);
					
					if (!shape.getDrawMode().equals(DrawMode.FILL))
					{
						this.imageContainer.setColor(shape.getContourColor());
						this.imageContainer.drawPolygon(pixelPolygon);
					}
				}
				else if (shape instanceof LineShape)
				{
					this.imageContainer.drawLine(((LineShape)shape).getPixelC0(),((LineShape)shape).getPixelC1());
					
					if (((LineShape)shape).isArrow())
					{
						Point c0 = ((LineShape)shape).getPixelC0();
						Point c1 = ((LineShape)shape).getPixelC1();
						
						
						double length = Math.hypot(c0.getX()-c1.getX(), c0.getY()-c1.getY());
						double dx = c1.getX() - c0.getX();
						double dy = c1.getY() - c0.getY();
						double nX = dx/length;
						double nY = dy/length;

						//shift arrow 10% of the link length to the left
						double rightShiftX = nY * .1 * length;
						double rightShiftY = -nX * .1 * length;

						//from-to arrow
						double tXA = c1.getX() + rightShiftX;
						double tYA = c1.getY() + rightShiftY;
						
						//arrow peak is 10% of link length long;
						double leftPeakEndX = tXA-.3*length*nX + -nY * .1 * length - rightShiftX*2;
						double leftPeakEndY = tYA-.3*length*nY + nX * .1 * length - rightShiftY*2;
						
						double rightPeakEndX = tXA-.3*length*nX - -nY * .1 * length;
						double rightPeakEndY = tYA-.3*length*nY - nX * .1 * length;
						
						Point a0 = new Point((int)leftPeakEndX, (int)leftPeakEndY);
						
						Point a1 = new Point((int)rightPeakEndX, (int)rightPeakEndY);
						
						this.imageContainer.drawLine(c1,a0);
						this.imageContainer.drawLine(c1,a1);
						
					}
					
//					this.imageContainer.fillCircle(((LineShape)shape).getPixelC0().x,((LineShape)shape).getPixelC0().y,10,10);
//					this.imageContainer.setColor(Color.red);
//					this.imageContainer.fillCircle(((LineShape)shape).getPixelC1().x,((LineShape)shape).getPixelC1().y,10,10);
				}
			}
			
			this.imageContainer.translate(controller.getViewportBounds().x, controller.getViewportBounds().y);
//			this.imageContainer.scale(1d/zoomFactor, 1d/zoomFactor);
			
		}
		
	}
	
	@Override
	public void updatePixelCoordinates(boolean all)
	{
		if (controller.getActiveShapes()==null)
			return;
		
		ArrayList<Shape> shapes = controller.getActiveShapes();
		
		if (all)
		{
			for (Shape shape : shapes)
				updatePixelCoordinates(shape);
		}
		else
		{
			for (Shape shape : shapes)
			{
				if (shape.getLayerID()==this.id)
					updatePixelCoordinates(shape);
			}
		}
	}

	public void updatePixelCoordinates(Shape shape)
	{
		
		if (shape instanceof BoxShape)
		{
			Rectangle pixelBox = controller.geoToPixel(((BoxShape)shape).getBox());
			
			if (((BoxShape)shape).isFixedSize())
			{
				pixelBox.x+= ((BoxShape)shape).getOffsetX();
				pixelBox.y+= ((BoxShape)shape).getOffsetY();
				pixelBox.width = ((BoxShape)shape).getFixedWidth();
				pixelBox.height = ((BoxShape)shape).getFixedHeight();
			}
			
			((BoxShape)shape).setPixelBox(pixelBox);
		}
		else if (shape instanceof CircleShape)
		{
			Point pixelOrigin = controller.geoToPixel(((CircleShape)shape).getOrigin());
			Point pixelDestination = controller.geoToPixel(((CircleShape)shape).getDestination());
			
			((CircleShape)shape).setPixelOrigin(pixelOrigin);
			((CircleShape)shape).setPixelDestination(pixelDestination);
			((CircleShape)shape).setPixelRadius(2*(int)Math.sqrt((pixelOrigin.x - pixelDestination.x)
															    *(pixelOrigin.x - pixelDestination.x)
															    +(pixelOrigin.y - pixelDestination.y)
															    *(pixelOrigin.y - pixelDestination.y)));
			
		}
		else if (shape instanceof PolygonShape)
		{
			
			Polygon polygon = ((PolygonShape)shape).getPolygon();
			java.awt.Polygon pixelPolygon = new java.awt.Polygon();
			
			for (int i = 0; i < polygon.getNumPoints(); i++)
			{
				Coordinate c = polygon.getExteriorRing().getCoordinateN(i);
				Point pixelPoint = controller.geoToPixel(new Point2D.Double(c.y, c.x));
				pixelPolygon.addPoint(pixelPoint.x, pixelPoint.y);
			}
			
			((PolygonShape)shape).setPixelPolygon(pixelPolygon);
			
//			System.out.println("x:"+ pixelPolygon.xpoints[1] + ",y:" + pixelPolygon.ypoints[1]);
			
		}
		else if (shape instanceof LineShape)
		{
			Point c0 = controller.geoToPixel(((LineShape)shape).getC0());
			Point c1 = controller.geoToPixel(((LineShape)shape).getC1());
			
			c0.x+=((LineShape)shape).getOffsetX();
			c0.y+=((LineShape)shape).getOffsetY();
			c1.x+=((LineShape)shape).getOffsetX();
			c1.y+=((LineShape)shape).getOffsetY();
			
//			System.out.println("line");
//			System.out.println("c0:" + c0 + " | coord: " + ((LineShape)shape).getC0());
//			System.out.println("c1:" + c1 + " | coord: " + ((LineShape)shape).getC1());
//			System.out.println("" + System.nanoTime());
			
			((LineShape)shape).setPixelC0(c0);
			((LineShape)shape).setPixelC1(c1);
		}
		
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		updatePixelCoordinates(false);
		super.setEnabled(enabled);
	}

}
