package playground.wdoering.grips.v2.popareaselector;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;

/**
 * the map event listeners
 * 
 * @author vvvvv
 *
 */
class PopEventListener extends AbstractListener
{
	private Rectangle viewPortBounds;
	private int border;
	private int offsetX;
	private int offsetY;
	private String currentCircleId;
	
	public PopEventListener(Controller controller)
	{
		super(controller);
		this.border = controller.getImageContainer().getBorderWidth();
		this.offsetX = this.border;
		this.offsetY = this.border;
		
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			//get origin
			this.controller.c0 = this.controller.pixelToGeo(getGeoPoint(e.getPoint()));
			this.controller.c1 = this.controller.c0;
			
			//set evacuation circle
			setPopCircle(this.controller.c0, this.controller.c1);
			
			//drawing a circle (goal is not achieved yet), repaint and fix view
			controller.getActiveToolBox().setGoalAchieved(false);
			controller.paintLayers();
			this.fixed = true;
			
			
		}
		
		super.mousePressed(e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e)
	{
		if (this.pressedButton == MouseEvent.BUTTON1)
		{
			//get destination
			this.controller.c1 = this.controller.pixelToGeo(getGeoPoint(e.getPoint()));
			
			//update circle
			CircleShape circle = (CircleShape)controller.getShapeById(this.currentCircleId);
			circle.setDestination(this.controller.c1);
			this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(circle);
			
			//repaint
			controller.paintLayers();
		}
		
		super.mouseDragged(e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		this.fixed = false;
		if (this.pressedButton == MouseEvent.BUTTON1)
		{
			Shape shape = controller.getShapeById(this.currentCircleId);
			if (shape instanceof CircleShape)
			{
				CircleShape circle = (CircleShape)shape;
				if (this.controller.c0 != this.controller.c1)
				{
					PolygonShape polygon = this.controller.getShapeUtils().getPolygonFromCircle(circle);
					polygon.putMetaData("population", "100");
					this.controller.addShape(polygon);
					this.controller.getActiveToolBox().updateMask();
					this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(polygon);
					controller.getActiveToolBox().setGoalAchieved(true);
					controller.paintLayers();
				}
				else
					this.controller.removeShape(circle.getId());
			}
		}
		
		super.mouseReleased(e);
	}

	public Point getGeoPoint(Point mousePoint)
	{
		viewPortBounds = this.controller.getViewportBounds();
		return new Point(mousePoint.x+viewPortBounds.x-offsetX,mousePoint.y+viewPortBounds.y-offsetY);
	}
	
	public void setPopCircle(Point2D c0, Point2D c1)
	{
		CircleShape popCircle = ShapeFactory.getPopShape(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), c0, c1);
		this.currentCircleId = popCircle.getId();
		controller.addShape(popCircle);
		this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(popCircle);

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		super.mouseWheelMoved(e);
		controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(true);
	}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
//		controller.paintLayers();
	}
	
	@Override
	public void updateMousePosition(Point point)
	{
		this.controller.setMousePosition(getGeoPoint(point));
		
	}
}