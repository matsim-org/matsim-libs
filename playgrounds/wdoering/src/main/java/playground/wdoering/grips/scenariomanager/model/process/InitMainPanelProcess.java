package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.view.DefaultRenderPanel;

public class InitMainPanelProcess extends BasicProcess {

	public InitMainPanelProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		//check if the default render panel is set
		if (!controller.hasDefaultRenderPanel())
			controller.setMainPanel(new DefaultRenderPanel(this.controller), true);		
	}
	

}
