package playground.wdoering.grips.v2.analysis;

import java.awt.event.MouseEvent;

import playground.wdoering.grips.scenariomanager.control.AbstractListener;
import playground.wdoering.grips.scenariomanager.control.Controller;

public class EAEventListener extends AbstractListener
{

	public EAEventListener(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		this.controller.paintLayers();
	}

}
