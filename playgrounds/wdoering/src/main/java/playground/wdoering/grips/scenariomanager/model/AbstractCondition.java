package playground.wdoering.grips.scenariomanager.model;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.view.Visualizer;

public class AbstractCondition implements ConditionInterface
{
	
	protected Controller controller;
	protected Visualizer visualizer;
	
	public AbstractCondition(Controller controller, Visualizer visualizer)
	{
		this.controller = controller;
		this.visualizer = visualizer;
	}

	@Override
	public boolean checkCondition()
	{
		return false;
	}

}
