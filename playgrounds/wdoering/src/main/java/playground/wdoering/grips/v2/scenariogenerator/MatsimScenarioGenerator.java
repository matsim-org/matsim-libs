package playground.wdoering.grips.v2.scenariogenerator;

import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.AbstractProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;

public class MatsimScenarioGenerator extends AbstractModule
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
		MatsimScenarioGenerator scenario = new MatsimScenarioGenerator(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		
//		try
//		{
//			Thread.sleep(100);
//		} catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// start the process chain
		scenario.start();
		frame.requestFocus();
	}

	public MatsimScenarioGenerator(Controller controller)
	{
		super(controller.getLocale().moduleMatsimScenarioGenerator(), Constants.ModuleType.MATSIMSCENARIO, controller);
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
	
	private class InitProcess extends AbstractProcess
	{

		private MSGMask msgMask;

		public InitProcess(AbstractModule module, Controller controller)
		{
			super(module, controller);
		}
		
		@Override
		public void start()
		{
			//in case this is only part of something bigger
			controller.disableAllRenderLayers();
			
//			DefaultWindow pc = (DefaultWindow)controller.getParentComponent();
			this.msgMask = new MSGMask(controller);
			this.controller.setMainPanel(msgMask, false);
			
//			pc.setMainPanel(this.msgMask);
			this.controller.setToolBoxVisible(false);

			
			// check if Matsim config (including the OSM network) has been loaded
			if (!controller.isMatsimConfigOpened())
				if (!controller.openMastimConfig())
					exit(locale.msgOpenMatsimConfigFailed());

			this.msgMask.readConfig();
//			//set tool box
//			if ((controller.getActiveToolBox()!=null))
//				addToolBox(null);
			
			
			//finally: enable all layers
			controller.enableAllRenderLayers();
			
//			this.controller.setMainPanel(new ScenGenToolBox(controller),false);
		}
		
	}
	

}
