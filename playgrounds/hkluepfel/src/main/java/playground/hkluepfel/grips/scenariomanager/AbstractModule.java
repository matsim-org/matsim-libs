package playground.hkluepfel.grips.scenariomanager;

import java.util.ArrayList;

public abstract class AbstractModule
{
	protected ArrayList<Process> processList;
	
	public static void main(String[] args)
	{
		
	}
	
	public AbstractModule()
	{
		processList = new ArrayList<Process>();
	}
	
	public ArrayList<Process> getProcessList()
	{
		return processList;
	}
	
	public void setProcessList(ArrayList<Process> processList)
	{
		this.processList = processList;
		
	}
	
	

}
