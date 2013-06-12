package playground.wdoering.grips.scenariomanager.model.process;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;

public class SetModuleListenerProcess extends BasicProcess
{
	
	private AbstractListener listener;

	public SetModuleListenerProcess(Controller controller, AbstractListener listener)
	{
		super(controller);
		this.listener = listener;
	}
	
	@Override
	public void start()
	{
		//set module listeners
		if ((controller.getListener()==null) || (!(controller.getListener().getClass().isInstance(listener))))
			setListeners(listener);
	}
	

}
