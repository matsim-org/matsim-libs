package playground.wdoering.grips.scenariomanager.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.locale.Locale;

public class AbstractToolBox extends JPanel implements ActionListener
{
	protected Controller controller;
	protected Locale locale;
	protected boolean goalAchieved;
	protected AbstractModule module;
	
	public AbstractToolBox(AbstractModule module, Controller controller)
	{
		this.module = module;
		this.controller = controller;
		this.locale = controller.getLocale();
		this.goalAchieved = false;
	}
	
	public void resetMask()
	{
		
	}
	
	public void updateMask()
	{
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		
	}
	
	public boolean isGoalAchieved()
	{
		return goalAchieved;
	}
	
	public void setGoalAchieved(boolean goalAchieved)
	{
		this.goalAchieved = goalAchieved;
	}
	
	
	public void fireSaveEvent()
	{
		this.actionPerformed(new ActionEvent(this, 0, locale.btSave()));
	}
	
	public Controller getController()
	{
		return controller;
	}

}
