package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public class InitSecondaryShapeLayerProcess extends BasicProcess {

	public InitSecondaryShapeLayerProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if there is already a secondary shape layer
		if (!controller.hasSecondaryShapeRenderer())
			this.controller.addRenderLayer(new ShapeRenderer(controller, controller.getImageContainer()));
	
	}
	
	

}
