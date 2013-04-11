package playground.wdoering.grips.scenariomanager.model;

import java.awt.Point;
import java.util.ArrayList;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.Visualizer;

public abstract class AbstractModule
{
	protected static int width = 1024;
	protected static int height = 768;
	protected static int border = 30;	
	
	protected ArrayList<ProcessInterface> processList;
	protected Controller controller;
	protected Point mousePosition;
	protected boolean mainGoalAchieved;
	
	protected String title;
	protected ModuleType moduleType;
	
	protected ArrayList<ModuleType> nextModules;
	private boolean enabled = false;
	
	public AbstractModule(String title, ModuleType moduleType, Controller controller)
	{
		this.title = title;
		this.moduleType = moduleType;
		this.controller = controller;
		this.processList = new ArrayList<ProcessInterface>();
		this.nextModules = Constants.getNextModules(moduleType);
		
		for (ModuleType nextmoduleType : nextModules)
			System.out.println("type:" + nextmoduleType.toString());
		System.out.println();
		
		//set tool box
		if (this.controller.isStandAlone())
		{
			controller.setActiveToolBox(getToolBox());
			this.enabled = true;
		}

		// add processes
		processList.add(getInitProcess());
		
		this.controller.setActiveModuleType(this.moduleType);
		
	}
	
	public void setNextModules(ArrayList<ModuleType> nextModules)
	{
		this.nextModules = nextModules;
	}
	
	public ArrayList<ModuleType> getNextModules()
	{
		return nextModules;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public ProcessInterface getInitProcess()
	{
		return null;
	}

	public AbstractToolBox getToolBox()
	{
		return null;
	}

	public ArrayList<ProcessInterface> getProcessList()
	{
		return processList;
	}
	
	public void setProcessList(ArrayList<ProcessInterface> processList)
	{
		this.processList = processList;
		
	}
	
	public void start()
	{
		if (this.processList.size() > 0)
			this.processList.get(0).start();
	}
	
	public void sleep(int millis)
	{
		try { Thread.sleep(millis); } catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	public boolean isMainGoalAchieved()
	{
		return this.mainGoalAchieved;
	}
	
	public void setMainGoalAchieved(boolean mainGoalAchieved)
	{
		this.mainGoalAchieved = mainGoalAchieved;
	}

	protected void exit(String exitString)
	{
		System.out.println(exitString);
		System.exit(0);
	}

	public ModuleType getModuleType()
	{
		return moduleType;
	}
	
	public void setModuleType(ModuleType moduleType)
	{
		this.moduleType = moduleType;
	}

	public void enableNextModules()
	{
		System.out.println("this: " + this.getModuleType());
		for (ModuleType nextModuleType : nextModules)
		{
			System.out.println("next: " + nextModuleType.toString());
			controller.enableModule(nextModuleType);
		}
		System.out.println();
		
		this.controller.updateParentUI();
	
			
		
	}

	public void setEnabled(boolean b)
	{
		this.enabled  = b;
		
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	
	
	

}
