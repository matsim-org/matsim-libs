package grips;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.v2.evacareaselector.EvacAreaSelector;

public class ScenarioManager extends playground.wdoering.grips.scenariomanager.ScenarioManager
{
	/**
	 * 
	 */

	private static final long serialVersionUID = 1L;

	public ScenarioManager(Controller controller) {
		super(controller);
		// TODO Auto-generated constructor stub
	}
	final Controller controller = new Controller();
	BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
	BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
	controller.setImageContainer(imageContainer);
	controller.setMainFrameUndecorated(false);
	
	controller.setStandAlone(false);
	
	ArrayList<AbstractModule> moduleChain = new ArrayList<AbstractModule>();
	//current work flow
	moduleChain.add(new EvacAreaSelector(controller));


}
