package playground.wdoering.grips.scenariomanager.model.process;

import java.awt.geom.Rectangle2D;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.view.DefaultRenderPanel;

public class InitBBShapeProcess extends BasicProcess {

	public InitBBShapeProcess(Controller controller) {
		super(controller);
	}

	@Override
	public void start() {
		// add network bounding box shape
		int primaryShapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
		Rectangle2D bbRect = controller.getBoundingBox();
		controller.addShape(ShapeFactory.getNetBoxShape(primaryShapeRendererId, bbRect, true));

	}

}
