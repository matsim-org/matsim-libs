package playground.wdoering.grips.scenariomanager.view.renderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EventListener;

import playground.wdoering.grips.scenariomanager.control.Controller;

public abstract class AbstractSlippyMapRenderLayer extends AbstractRenderLayer
{

	public AbstractSlippyMapRenderLayer(Controller controller)
	{
		super(controller);
	}
	
	public ArrayList<EventListener> getInheritedEventListeners()
	{
		return null;
	}
	
	public Rectangle getViewportBounds()
	{
		return null;
	}

	public int getZoom()
	{
		return 0;
	}


	public Point2D pixelToGeo(Point2D point)
	{
		return null;
	}

	public Point geoToPixel(Point2D point)
	{
		return null;
	}

	public void setZoom(int zoom)
	{
				
	}
	
	public Rectangle getBounds()
	{
		return null;

	}

	public void setPosition(Point2D position)
	{
		
	}

}
