package playground.wdoering.grips.v2.ptlines;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractProcess;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.ProcessInterface;
import playground.wdoering.grips.scenariomanager.view.DefaultRenderPanel;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public class PTLEditor extends AbstractModule
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
		AbstractModule publicTransitLinesEditor = new PTLEditor(controller);
		
		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		publicTransitLinesEditor.start();
		frame.requestFocus();
	}

	public PTLEditor(Controller controller)
	{
		super(controller.getLocale().modulePTLEditor(), Constants.ModuleType.BUSSTOPS, controller);
	}
	
	@Override
	public AbstractToolBox getToolBox()
	{
		return new PTLToolBox(this, this.controller);
	}
	
	@Override
	public ProcessInterface getInitProcess()
	{
		return new PTLInitProcess(this, this.controller);
	}
	
	private class PTLInitProcess extends AbstractProcess
	{

		public PTLInitProcess(AbstractModule module, Controller controller)
		{
			super(module, controller);
		}
		
		@Override
		public void start()
		{

			//in case this is only part of something bigger
			controller.disableAllRenderLayers();
			
			// check if Matsim config (including the OSM network) has been loaded
			if (!controller.isMatsimConfigOpened())
				if (!controller.openMastimConfig())
					exit(locale.msgOpenMatsimConfigFailed());
			
			//check if the default render panel is set
			if (!controller.hasDefaultRenderPanel())
				controller.setMainPanel(new DefaultRenderPanel(this.controller), true);

			// check if there is already a map viewer running, or just (re)set center position
			if (!controller.hasMapRenderer())
				addMapViewer();
			else
				controller.getVisualizer().getActiveMapRenderLayer().setPosition(controller.getCenterPosition());
			
			// check if there is already a primary shape layer
			if (!controller.hasShapeRenderer())
				addShapeRenderer(new ShapeRenderer(controller, controller.getImageContainer()));
			
			// check if there is already a secondary shape layer
			if (!controller.hasSecondaryShapeRenderer())
				addShapeRenderer(new ShapeRenderer(controller, controller.getImageContainer()));
			
			//set module listeners
			if ((controller.getListener()==null) || (!(controller.getListener() instanceof PTLEventListener)) )
				setListeners(new PTLEventListener(controller));

			// check if Grips config (including the OSM network) has been loaded
			if (!controller.openEvacuationShape(Constants.ID_EVACAREAPOLY))
				exit(locale.msgOpenEvacShapeFailed());
			
			if (!controller.isPopulationFileOpened())
			{
				controller.openPopulationFile();
				System.out.println("its not ?");
			}
			
			controller.openNetworkChangeEvents();
			
//			// check if there is a network layer
//			if (!controller.hasNetworkRenderer())
//				addNetworkRenderer(new NetworkRenderer(controller));
			
			//validate render layers
			this.controller.validateRenderLayers();

			//add network bounding box shape
			int primaryShapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
			Rectangle2D bbRect = controller.getBoundingBox();
			controller.addShape(ShapeFactory.getNetBoxShape(primaryShapeRendererId, bbRect, true));
			
			//set tool box
			if ((controller.getActiveToolBox()==null) || (!(controller.getActiveToolBox() instanceof PTLToolBox)))
				addToolBox(new PTLToolBox(this.module, controller));
			
			//finally: enable all layers
			controller.enableAllRenderLayers();			
			
			
		}
		
	}

}
