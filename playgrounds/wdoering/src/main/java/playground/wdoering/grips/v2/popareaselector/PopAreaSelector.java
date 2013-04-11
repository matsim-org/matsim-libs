package playground.wdoering.grips.v2.popareaselector;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.AbstractProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;
import playground.wdoering.grips.scenariomanager.view.DefaultRenderPanel;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public class PopAreaSelector extends AbstractModule
{
	
	private PopToolBox toolBox;
	
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
		AbstractModule popAreaSelector = new PopAreaSelector(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		popAreaSelector.start();
		frame.requestFocus();
		
	}	

	public PopAreaSelector(Controller controller)
	{
		super(controller.getLocale().modulePopAreaSelector(), Constants.ModuleType.POPULATION, controller);
	}
	
	@Override
	public AbstractToolBox getToolBox()
	{
		if (toolBox == null)
			toolBox = new PopToolBox(this, this.controller);
		
		return toolBox;
	}
	
	@Override
	public ProcessInterface getInitProcess()
	{
		return new PopInitProcess(this, this.controller);
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
	private class PopInitProcess extends AbstractProcess
	{
		public PopInitProcess(AbstractModule module, Controller controller)
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
			{
				if (!controller.openGripsConfig())
					exit(locale.msgOpenGripsConfigFailed());
			}
			else
			{
				if (PopAreaSelector.this.toolBox!=null)
					PopAreaSelector.this.toolBox.updateMask();
			}
//				for (Shape shape : this.controller.getActiveShapes())
//				{
//				}
			
			//check if the default render panel is set
			if (!controller.hasDefaultRenderPanel())
				controller.setMainPanel(new DefaultRenderPanel(this.controller), true);

			// check if there is already a map viewer running, or just (re)set center position
			if (!controller.hasMapRenderer())
				addMapViewer();
			else
				controller.getVisualizer().getActiveMapRenderLayer().setPosition(controller.getCenterPosition());
			
			//set module listeners
			if ((controller.getListener()==null) || (!(controller.getListener() instanceof PopEventListener)) )
				setListeners(new PopEventListener(controller));

			// check if there is already a primary shape layer
			if (!controller.hasShapeRenderer())
				addShapeRenderer(new ShapeRenderer(controller, controller.getImageContainer()));
			
			// check if Grips config (including the OSM network) has been loaded
			if (!controller.openEvacuationShape(Constants.ID_EVACAREAPOLY))
				exit(locale.msgOpenEvacShapeFailed());
			
			//validate render layers
			this.controller.validateRenderLayers();

//			//add network bounding box shape
			int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
			Rectangle2D bbRect = controller.getBoundingBox();
			controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, true));
			
			//set tool box
			if ((controller.getActiveToolBox()==null) || (!(controller.getActiveToolBox() instanceof PopToolBox)))
				addToolBox(new PopToolBox(module, controller));
			
			//finally: enable all layers
			controller.enableAllRenderLayers();
		}

	}
	
	

}
