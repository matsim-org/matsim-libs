package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;

public class DisableLayersProcess extends BasicProcess
{

	public DisableLayersProcess(Controller controller) 
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		controller.disableAllRenderLayers();
	}


}
