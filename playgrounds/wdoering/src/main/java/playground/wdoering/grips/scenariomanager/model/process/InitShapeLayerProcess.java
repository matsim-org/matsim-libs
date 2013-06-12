package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public class InitShapeLayerProcess extends BasicProcess {

	public InitShapeLayerProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if there is already a primary shape layer
		if (!controller.hasShapeRenderer())
			this.controller.addRenderLayer(new ShapeRenderer(controller, controller.getImageContainer()));
		
	}
	
	

}
