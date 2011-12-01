package playground.wdoering.debugvisualization.model;

import java.util.HashMap;

@Deprecated
public class Scene
{
	double time;
	HashMap<String, DataPoint> agents;
	
	
	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public Scene(double time)
	{
		this.time = time;
		this.agents = new HashMap<String,DataPoint>();
	}
	
	public void setAgentDataPoint(String ID, DataPoint dataPoint)
	{
	 	agents.put(ID, dataPoint);
	}
	
}
