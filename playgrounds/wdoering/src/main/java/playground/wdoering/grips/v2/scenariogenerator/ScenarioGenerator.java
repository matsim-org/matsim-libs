package playground.wdoering.grips.v2.scenariogenerator;

import java.awt.Color;
import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.BasicProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;
import playground.wdoering.grips.v2.popareaselector.PopAreaSelector;

public class ScenarioGenerator extends AbstractModule
{
	
	public static void main(String[] args)
	{
		final Controller controller = new Controller();
		
		// set up controller and image interface
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);
		
		// instantiate evacuation area selector
		ScenarioGenerator scenario = new ScenarioGenerator(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);

		// start the process chain
		scenario.start();
		frame.requestFocus();
	}

	public ScenarioGenerator(Controller controller)
	{
		super(controller.getLocale().moduleScenarioGenerator(), Constants.ModuleType.GRIPSSCENARIO, controller);
		this.processList.add(getInitProcess());
	}
	
	@Override
	public ProcessInterface getInitProcess()
	{
		return new InitProcess(this, controller);
	}
	
	@Override
	public AbstractToolBox getToolBox()
	{
		return null;
	}
	
	private class InitProcess extends BasicProcess
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
			
			//create scenario generator mask, disable toolbox
			SGMask mask = new SGMask(ScenarioGenerator.this, controller);
			this.controller.setMainPanel(mask, false);
			this.controller.setToolBoxVisible(false);
			
			// check if Grips config (including the OSM network) has been loaded
			if (!controller.isGripsConfigOpenend())
				if (!controller.openGripsConfig())
					exit(locale.msgOpenGripsConfigFailed());
			
			mask.enableRunButton(true);
			
			//finally: enable all layers
			controller.enableAllRenderLayers();
			
//			this.controller.setMainPanel(new ScenGenToolBox(controller),false);
		}
		
	}
	

}
