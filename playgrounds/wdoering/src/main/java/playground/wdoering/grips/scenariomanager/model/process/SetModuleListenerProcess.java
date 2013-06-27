package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;

public class SetModuleListenerProcess extends BasicProcess
{
	
	private AbstractListener listener;
	private AbstractModule module;

	public SetModuleListenerProcess(Controller controller, AbstractModule module, AbstractListener listener)
	{
		super(controller);
		this.listener = listener;
		this.module = module;
	}
	
	@Override
	public void start()
	{
		//set module listeners
		if ((controller.getListener()==null) || (!(controller.getListener().getClass().isInstance(listener))))
			setListeners(listener);
		
		module.setListener(listener);
	}
	

}
