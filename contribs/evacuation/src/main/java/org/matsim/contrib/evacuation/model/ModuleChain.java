package org.matsim.contrib.evacuation.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.contrib.evacuation.model.Constants.ModuleType;

/**
 * class to describe the flow of modules.
 * nextModules defines modules to activate,
 * whilte pastModules defines the ones to
 * deactivate.
 * 
 * @author wdoering
 *
 */
public abstract class ModuleChain
{
	
	protected HashMap<ModuleType,ArrayList<ModuleType>> nextModules;
	protected HashMap<ModuleType,ArrayList<ModuleType>> pastModules;

	public ModuleChain()
	{
		this.nextModules = new HashMap<Constants.ModuleType, ArrayList<ModuleType>>();
		this.pastModules = new HashMap<Constants.ModuleType, ArrayList<ModuleType>>();
	}
	
	public ArrayList<ModuleType> getNextModules(ModuleType module)
	{
		return this.nextModules.get(module);
	}
	
	public ArrayList<ModuleType> getPastModules(ModuleType module)
	{
		return this.pastModules.get(module);
	}

}
