package playground.wdoering.grips.scenariomanager.control.eventlistener;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import playground.wdoering.grips.scenariomanager.control.Controller;

public abstract class AbstractMouseWheelEventListener implements MouseWheelListener
{
	protected Controller controller;
	
	public AbstractMouseWheelEventListener(Controller controller)
	{
		this.controller = controller;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		for (MouseWheelListener mw : controller.getMouseWheelListener())
			mw.mouseWheelMoved(e);
	}

}
