package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.v2.analysis.EAToolBox;

public class SetToolBoxProcess extends BasicProcess
{
	
	private AbstractToolBox toolBox;
	private Class toolboxInstance;
	
	public SetToolBoxProcess(Controller controller, AbstractToolBox toolBox)
	{
		super(controller);
		this.toolBox = toolBox;
	}
	
	@Override
	public void start()
	{
		
		System.out.println("!active toolbox: " + controller.getActiveToolBox());
		
		//set tool box
		if ((controller.getActiveToolBox()==null) || (!(controller.getActiveToolBox().getClass().isInstance(toolBox))))
			this.controller.setActiveToolBox(toolBox);
	}	

}
