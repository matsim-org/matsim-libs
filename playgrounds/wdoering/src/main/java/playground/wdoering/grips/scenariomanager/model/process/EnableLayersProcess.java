package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;

public class EnableLayersProcess extends BasicProcess
{

	public EnableLayersProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		System.out.println("enabling all layers");
		controller.enableAllRenderLayers();
	}
	

}
