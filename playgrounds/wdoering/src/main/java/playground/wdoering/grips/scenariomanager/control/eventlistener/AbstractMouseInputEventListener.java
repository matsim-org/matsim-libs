package playground.wdoering.grips.scenariomanager.control.eventlistener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.event.MouseInputListener;

import playground.wdoering.grips.scenariomanager.control.Controller;

public abstract class AbstractMouseInputEventListener implements MouseInputListener
{
	protected Controller controller;
	
	public AbstractMouseInputEventListener(Controller controller)
	{
		this.controller = controller;
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
//		controller.paintLayers();
		for (MouseListener m : controller.getMouseListener())
			m.mouseClicked(e);
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mousePressed(e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mouseReleased(e);
	}
	
	@Override
	public void mouseEntered(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mouseEntered(e);
	}
	
	@Override
	public void mouseExited(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mouseExited(e);
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		for (MouseMotionListener mm : controller.getMouseMotionListener())
			mm.mouseDragged(e);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		for (MouseMotionListener mm : controller.getMouseMotionListener())
			mm.mouseMoved(e);
	}
}