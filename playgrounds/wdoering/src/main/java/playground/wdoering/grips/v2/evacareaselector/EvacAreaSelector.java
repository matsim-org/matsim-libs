package playground.wdoering.grips.v2.evacareaselector;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.BasicProcess;
import playground.wdoering.grips.scenariomanager.model.process.DisableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.EnableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitGripsConfigProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMainPanelProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMapLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitShapeLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;
import playground.wdoering.grips.scenariomanager.model.process.SetModuleListenerProcess;
import playground.wdoering.grips.scenariomanager.model.process.SetToolBoxProcess;
import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;

/**
 * 
 * <code>InitProcess</code> <code>InnerEvacAreaListener</code>
 *  
 * @author wdoering
 * 
 */
public class EvacAreaSelector extends AbstractModule
{

	public static void main(String[] args)
	{
		// set up controller and image interface
		final Controller controller = new Controller(args);
		controller.setImageContainer(BufferedImageContainer.getImageContainer(width, height, border));

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
		
		
		//disable all layers
		this.processList.add(new DisableLayersProcess(controller));
		
		//initialize GRIPS config
		this.processList.add(new InitGripsConfigProcess(controller));

		//check if the default render panel is set
		this.processList.add(new InitMainPanelProcess(controller));
		
		// check if there is already a map viewer running, or just (re)set center position
		this.processList.add(new InitMapLayerProcess(controller));
		
		//set module listeners		
		this.processList.add(new SetModuleListenerProcess(controller, this, new EvacEventListener(controller)));
		
		// check if there is already a primary shape layer
		this.processList.add(new InitShapeLayerProcess(controller));
		
		//add bounding box
		this.processList.add(new BasicProcess(controller)
		{
			@Override
			public void start()
			{
				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, false));
			}

		});
		
		//add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));
		
		//enable all layers
		this.processList.add(new EnableLayersProcess(controller));
		
		System.out.println("processes queued:" + processList.size());
		for (ProcessInterface process : processList)
			System.out.println(process.toString());
		
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
		return null;
//		return new InitProcess(this, this.controller);
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
