package playground.wdoering.grips.scenariomanager.view;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;
import playground.wdoering.grips.scenariomanager.view.renderer.AbstractRenderLayer;
import playground.wdoering.grips.scenariomanager.view.renderer.AbstractSlippyMapRenderLayer;
import playground.wdoering.grips.scenariomanager.view.renderer.GridRenderer;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public class Visualizer
{
	
	private ArrayList<AbstractRenderLayer> renderLayers;
	private Controller controller;
	private AbstractSlippyMapRenderLayer activeMapRenderLayer;
	private ShapeRenderer primaryShapeRenderLayer;
	private ShapeRenderer secondaryShapeRenderLayer;
	private AtomicBoolean b = new AtomicBoolean(false);
	private boolean painting = false;
	
	public Visualizer(Controller controller)
	{
		this.controller = controller;
		this.renderLayers = new ArrayList<AbstractRenderLayer>();
	}
	
	public ArrayList<AbstractRenderLayer> getRenderLayers()
	{
		return renderLayers;
	}
	
	public boolean hasMapRenderer()
	{
		for (AbstractRenderLayer layer : this.getRenderLayers())
		{
			if (layer instanceof AbstractSlippyMapRenderLayer)
				return true;
		}
		return false;
	}
	
	public boolean hasGridRenderer()
	{
		for (AbstractRenderLayer layer : this.getRenderLayers())
		{
			if (layer instanceof GridRenderer)
				return true;
		}
		return false;		
	}
	
	public boolean hasShapeRenderer()
	{
		// shape renderer is at hand
		if (this.getPrimaryShapeRenderLayer() != null)
			return true;
		
		// if not, check all layers again, just to be sure
		for (AbstractRenderLayer layer : this.getRenderLayers())
		{
			if (layer instanceof ShapeRenderer)
				return true;
		}		
		
		return false;
	}

	
	public void setRenderLayers(ArrayList<AbstractRenderLayer> renderLayers)
	{
		this.renderLayers = renderLayers;
	}
	
	public void addRenderLayer(AbstractRenderLayer layer)
	{
		if (layer instanceof AbstractSlippyMapRenderLayer)
			this.activeMapRenderLayer = (AbstractSlippyMapRenderLayer)layer;
		
		if ((this.primaryShapeRenderLayer==null) && (layer instanceof ShapeRenderer))
			this.primaryShapeRenderLayer = (ShapeRenderer)layer;
		
		if ((this.primaryShapeRenderLayer!=null) && (layer instanceof ShapeRenderer))
			this.secondaryShapeRenderLayer = (ShapeRenderer)layer;
		
		this.renderLayers.add(layer);
	}
	
	public boolean removeRenderLayer(AbstractRenderLayer layerToRemove)
	{
		for (int i = 0; i < renderLayers.size(); i++)
		{
			
			if (renderLayers.get(i).equals(layerToRemove))
			{
				if (renderLayers.get(i) instanceof AbstractSlippyMapRenderLayer)
					this.activeMapRenderLayer = null;
				
				renderLayers.remove(i);
				return true;
			}
		}
		return false;
	}
	
	public void removeAllLayers(boolean leaveMapRenderer)
	{
		if (leaveMapRenderer)
		{
			ArrayList<AbstractRenderLayer> newLayers = new ArrayList<AbstractRenderLayer>();
			for (AbstractRenderLayer layer : renderLayers)
			{
				if (layer instanceof AbstractSlippyMapRenderLayer)
					newLayers.add(layer);
			}
			renderLayers = newLayers;
		}
		else
			this.renderLayers = new ArrayList<AbstractRenderLayer>();
	}
	
	public synchronized void paintLayers()
	{
		
		painting = true;
		
		for (AbstractRenderLayer layer : renderLayers)
			layer.paintLayer();
		
		painting = false;
	}
	
	public BufferedImage getBufferedImage()
	{
		return controller.getImageContainer().getImage();
	}
	
	public synchronized ArrayList<Shape> getActiveShapes()
	{
		return this.controller.getActiveShapes();
	}

	public AbstractSlippyMapRenderLayer getActiveMapRenderLayer()
	{
		return activeMapRenderLayer;
	}
	
	public ShapeRenderer getPrimaryShapeRenderLayer()
	{
		return primaryShapeRenderLayer;
	}
	
	public ShapeRenderer getSecondaryShapeRenderLayer()
	{
		return secondaryShapeRenderLayer;
	}
	
	public boolean isPainting()
	{
		return painting;
	}
	
	public void setPainting(boolean painting)
	{
		this.painting = painting;
	}

	public boolean hasSecondaryShapeRenderer()
	{
		int count = 0;
		for (AbstractRenderLayer layer : this.getRenderLayers())
			if (layer instanceof ShapeRenderer)
				count++;
		return (count>1);
	}
	
	

}
