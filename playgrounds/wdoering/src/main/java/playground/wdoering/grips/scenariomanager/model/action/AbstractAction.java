package playground.wdoering.grips.scenariomanager.model.action;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.view.Visualizer;

public abstract class AbstractAction implements ActionInterface
{
	protected Controller controller;
	
	public AbstractAction(Controller controller)
	{
		this.controller = controller;
	}
	
}
