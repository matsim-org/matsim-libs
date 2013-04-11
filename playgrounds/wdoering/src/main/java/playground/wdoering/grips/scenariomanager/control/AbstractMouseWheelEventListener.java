package playground.wdoering.grips.scenariomanager.control;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

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
