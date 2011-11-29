package playground.wdoering.debugvisualization.controller;

import java.util.HashMap;

import playground.wdoering.debugvisualization.model.Agent;



public class AgentDataController  {
	private HashMap<String,Agent> agents = null;

	public void AgentDatacontroller(HashMap<String,Agent> agents)
	{
		this.agents = agents;
	}
	
	public synchronized HashMap<String,Agent> getAgents()
	{
		return agents;
	}
	
	public synchronized void setAgents(HashMap<String,Agent> agents)
	{
		this.agents = agents;
	}

	public boolean isAgentDataSet()
	{
		if (this.agents != null)
			return true;
		else
			return false;
			
	}
	

}
