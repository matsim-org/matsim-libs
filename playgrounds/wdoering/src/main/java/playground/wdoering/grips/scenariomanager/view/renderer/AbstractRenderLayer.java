package playground.wdoering.grips.scenariomanager.view.renderer;

import java.util.List;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.ImageContainerInterface;

public abstract class AbstractRenderLayer
{
	protected int id;
	protected final Controller controller;
	protected final ImageContainerInterface imageContainer;
	protected boolean enabled = false;
	
	protected static int currentId = -1;
	
	public AbstractRenderLayer(Controller controller)
	{
		this.controller = controller;
		this.imageContainer = controller.getImageContainer();
		this.id = ++currentId;
	}
	
	public ImageContainerInterface getImageContainer()
	{
		return imageContainer;
	}
	
	public static int getCurrentId()
	{
		return currentId;
	}
	
	public int getId()
	{
		return id;
	}
	
	private void setId(int id)
	{
		this.id = id;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	public void updatePixelCoordinates(boolean all)
	{
		return;
	}

	public synchronized void paintLayer() {}
}
