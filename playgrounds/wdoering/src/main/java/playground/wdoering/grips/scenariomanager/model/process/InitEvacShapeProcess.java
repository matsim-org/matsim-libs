package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public class InitEvacShapeProcess extends BasicProcess {
	
	private Shape shape;

	public InitEvacShapeProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if Grips config (including the OSM network) has been loaded
		if (!controller.openEvacuationShape(Constants.ID_EVACAREAPOLY))
			controller.exit(locale.msgOpenEvacShapeFailed());
	
	}
	
	

}
