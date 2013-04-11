package playground.wdoering.grips.v2.evacareaselector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.AbstractProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;
import playground.wdoering.grips.scenariomanager.model.shape.BoxShape;
import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.ShapeStyle;
import playground.wdoering.grips.scenariomanager.model.shape.Shape.DrawMode;
import playground.wdoering.grips.scenariomanager.view.DefaultRenderPanel;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

/**
 * 
 * <code>InitProcess</code> <code>InnerEvacAreaListener</code>
 *  
 * @author vvvvv
 * 
 */
public class EvacAreaSelector extends AbstractModule
{

	public static void main(String[] args)
	{
		// set up controller and image interface
		final Controller controller = new Controller();
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);
		
		// instantiate evacuation area selector
		AbstractModule evacAreaSelector = new EvacAreaSelector(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		evacAreaSelector.start();
		frame.requestFocus();
		
	}

	public EvacAreaSelector(Controller controller)
	{
		super(controller.getLocale().moduleEvacAreaSelector(), Constants.ModuleType.EVACUATION, controller);
	}
	
	@Override
	public AbstractToolBox getToolBox()
	{
		if (this.toolBox == null)
			this.toolBox = new EvacToolBox(this, this.controller);
		
		return this.toolBox;
	}
	
	@Override
	public ProcessInterface getInitProcess()
	{
		return new InitProcess(this, this.controller);
	}

	/**
	 * Initializing process
	 * 
	 * - check if grips config / osm xml network is loaded; if not, load it -
	 * check if a slippy map viewer has been initialized
	 * 
	 * 
	 * @author vvvvv
	 * 
	 */
	private class InitProcess extends AbstractProcess
	{
		public InitProcess(AbstractModule module, Controller controller)
		{
			super(module, controller);
		}

		@Override
		public void start()
		{
			//in case this is only part of something bigger
			controller.disableAllRenderLayers();
			
			// check if Grips config (including the OSM network) has been loaded
			if (!controller.isGripsConfigOpenend())
				if (!controller.openGripsConfig())
					exit(locale.msgOpenGripsConfigFailed());

			//check if the default render panel is set
			if (!controller.hasDefaultRenderPanel())
				controller.setMainPanel(new DefaultRenderPanel(this.controller), true);
			
			// check if there is already a map viewer running, or just (re)set center position
			if (!controller.hasMapRenderer())
				addMapViewer();
			else
				controller.getVisualizer().getActiveMapRenderLayer().setPosition(controller.getCenterPosition());
			
			//set module listeners
			if ((controller.getListener()==null) || (!(controller.getListener() instanceof EvacEventListener)) )
				setListeners(new EvacEventListener(controller));

			// check if there is already a primary shape layer
			if (!controller.hasShapeRenderer())
				addShapeRenderer(new ShapeRenderer(controller, controller.getImageContainer()));
			
			//validate render layers
			this.controller.validateRenderLayers();

			//add network bounding box shape
			int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
			Rectangle2D bbRect = controller.getBoundingBox();
			controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, false));
			
			System.out.println("active toolbox: " + controller.getActiveToolBox());
			//set tool box
			if ((controller.getActiveToolBox()==null) || (!(controller.getActiveToolBox() instanceof EvacToolBox)))
				addToolBox(new EvacToolBox(this.module, controller));
			
			
			//finally: enable all layers
			controller.enableAllRenderLayers();
		}

	}
	
	public Point getGeoPoint(Point mousePoint)
	{
		Rectangle viewPortBounds = this.controller.getViewportBounds();
		return new Point(mousePoint.x+viewPortBounds.x-offsetX,mousePoint.y+viewPortBounds.y-offsetY);
	}
	
	public void setEvacCircle(Point2D c0, Point2D c1)
	{
		CircleShape evacCircle = ShapeFactory.getEvacCircle(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), c0, c1);
		controller.addShape(evacCircle);
		this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(evacCircle);

	}
	
	
	
	

}
