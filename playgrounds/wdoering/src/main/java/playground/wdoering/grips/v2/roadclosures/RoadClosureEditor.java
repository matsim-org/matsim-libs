package playground.wdoering.grips.v2.roadclosures;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.BasicProcess;
import playground.wdoering.grips.scenariomanager.model.process.DisableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.EnableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitEvacShapeProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMainPanelProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMapLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMatsimConfigProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitSecondaryShapeLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitShapeLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;
import playground.wdoering.grips.scenariomanager.model.process.SetModuleListenerProcess;
import playground.wdoering.grips.scenariomanager.model.process.SetToolBoxProcess;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;

public class RoadClosureEditor extends AbstractModule
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
		AbstractModule roadClosureEditor = new RoadClosureEditor(controller);
		
		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		roadClosureEditor.start();
		frame.requestFocus();
	}

	public RoadClosureEditor(Controller controller)
	{
		super(controller.getLocale().moduleRoadClosureEditor(), Constants.ModuleType.ROADCLOSURE, controller);
		
		//disable all layers
		this.processList.add(new DisableLayersProcess(controller));
		
		//initialize Matsim config
		this.processList.add(new InitMatsimConfigProcess(controller));

		//check if the default render panel is set
		this.processList.add(new InitMainPanelProcess(controller));
		
		// check if there is already a map viewer running, or just (re)set center position
		this.processList.add(new InitMapLayerProcess(controller));
		
		//check if the default shape layer is set
		this.processList.add(new InitShapeLayerProcess(controller));
		
		//check if the secondary shape layer is set
		this.processList.add(new InitSecondaryShapeLayerProcess(controller));
		
		//set module listeners		
		this.processList.add(new SetModuleListenerProcess(controller, this, new RCEEventListener(controller)));
		
		//load evacuation area shape		
		this.processList.add(new InitEvacShapeProcess(controller));
		
		//add bounding box
		this.processList.add(new BasicProcess(controller)
		{
			@Override
			public void start()
			{

				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, true));
			}

		});		
		
		//add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));
		
		//enable all layers
		this.processList.add(new EnableLayersProcess(controller));
		

	}
	
	@Override
	public AbstractToolBox getToolBox()
	{
		if (toolBox == null)
			toolBox = new RCEToolBox(this, this.controller);

			return toolBox;
	}
	
	@Override
	public ProcessInterface getInitProcess()
	{
		return new RCEInitProcess(this, this.controller);
	}
	
	private class RCEInitProcess extends BasicProcess
	{

		public RCEInitProcess(AbstractModule module, Controller controller)
		{
			super(module, controller);
		}
		
		@Override
		public void start()
		{

//			//in case this is only part of something bigger
//			controller.disableAllRenderLayers();
//			
//			// check if Matsim config (including the OSM network) has been loaded
//			if (!controller.isMatsimConfigOpened())
//				if (!controller.openMastimConfig())
//					exit(locale.msgOpenMatsimConfigFailed());
//			
//			//check if the default render panel is set
//			if (!controller.hasDefaultRenderPanel())
//				controller.setMainPanel(new DefaultRenderPanel(this.controller), true);
//
////			// check if there is already a map viewer running, or just (re)set center position
////			if (!controller.hasMapRenderer())
////				addMapViewer();
////			else
////				controller.getVisualizer().getActiveMapRenderLayer().setPosition(controller.getCenterPosition());
//			new InitMapLayerProcess(controller).start();
//			
////			// check if there is already a primary shape layer
////			if (!controller.hasShapeRenderer())
////				addShapeRenderer(new ShapeRenderer(controller, controller.getImageContainer()));
////			
////			// check if there is already a secondary shape layer
////			if (!controller.hasSecondaryShapeRenderer())
////				addShapeRenderer(new ShapeRenderer(controller, controller.getImageContainer()));
//			
//			//set module listeners
//			if ((controller.getListener()==null) || (!(controller.getListener() instanceof RCEEventListener)) )
//				setListeners(new RCEEventListener(controller));
//
//			// check if Grips config (including the OSM network) has been loaded
//			if (!controller.openEvacuationShape(Constants.ID_EVACAREAPOLY))
//				exit(locale.msgOpenEvacShapeFailed());
//			
//			//validate render layers
//			this.controller.validateRenderLayers();
//
//			//add network bounding box shape
//			int primaryShapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
//			Rectangle2D bbRect = controller.getBoundingBox();
//			controller.addShape(ShapeFactory.getNetBoxShape(primaryShapeRendererId, bbRect, true));
//			
//			//set tool box
//			if ((controller.getActiveToolBox()==null) || (!(controller.getActiveToolBox() instanceof RCEToolBox)))
//				addToolBox(new RCEToolBox(this.module, controller));
//			this.controller.setToolBoxVisible(true);
//			
//			//finally: enable all layers
//			controller.enableAllRenderLayers();			
			
			
		}
		
	}

}
