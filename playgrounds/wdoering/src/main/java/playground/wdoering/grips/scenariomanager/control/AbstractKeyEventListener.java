package playground.wdoering.grips.scenariomanager.control;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public abstract class AbstractKeyEventListener implements KeyListener
{
	
	protected Controller controller;
	
	public AbstractKeyEventListener(Controller controller)
	{
		this.controller = controller;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		for (KeyListener k : controller.getKeyListener())
			k.keyTyped(e);
		
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		for (KeyListener k : controller.getKeyListener())
			k.keyPressed(e);
		
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		for (KeyListener k : controller.getKeyListener())
			k.keyReleased(e);
		
	}
	

}
