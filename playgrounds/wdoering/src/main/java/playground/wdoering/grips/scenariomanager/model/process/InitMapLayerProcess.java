package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.view.renderer.JXMapRenderer;

public class InitMapLayerProcess extends BasicProcess {

	public InitMapLayerProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if there is already a map viewer running, or just (re)set center position
		if (!controller.hasMapRenderer())
		{
			//add new jx map viewer interface
			JXMapRenderer jxMapRenderer = new JXMapRenderer(controller, controller.getWMS(), controller.getWMSLayer());
			controller.addRenderLayer(jxMapRenderer);
			controller.setSlippyMapEventListeners(jxMapRenderer.getInheritedEventListeners());
		}
		else
			controller.getVisualizer().getActiveMapRenderLayer().setPosition(controller.getCenterPosition());
	}
	

	

}
