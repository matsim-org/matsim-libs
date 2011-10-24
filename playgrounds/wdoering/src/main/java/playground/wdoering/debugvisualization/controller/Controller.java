package playground.wdoering.debugvisualization.controller;


import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import playground.wdoering.debugvisualization.model.Scene;
import playground.wdoering.debugvisualization.gui.*;



/**
 * Debug Visualization - controller
 * 
 * load data and set up console and GUI.
 * 
 * @author wdoering
 *
 */
public class Controller {

	private static final long timeInterval = 30;
	private static final int width = 600;
	private static final int height = 600;
	
	private GUI gui;
	private Importer importer;
	private HashMap<String,Agent> agents;
	private HashMap<Integer,DataPoint> nodes;
	private HashMap<Integer,int[]> links;
	private Double[] extremeValues;
	private Double maxPosX, minPosX, maxPosY, minPosY;
	private LinkedList<Double> timeSteps = new LinkedList<Double>();
	private boolean liveMode;
	private Double currentTime = Double.NaN;
	private Scene currentScene;
	private int traceTimeRange = 2;
	
	public Console console;
	private Long oldTime = 0l;
	
	public boolean isLiveMode() {
		return liveMode;
	}

	public void setLiveMode(boolean liveMode) {
		this.liveMode = liveMode;
	}

	public Controller(String eventFileName, String networkFileName, Console console, int traceTimeRange, boolean liveMode)
	{
		
		maxPosX = maxPosY = Double.MIN_VALUE;
		minPosX = minPosY = Double.MAX_VALUE;
		
		
		this.liveMode = liveMode;
		this.traceTimeRange = traceTimeRange;
		

		//set up importer. can surely be replaced.
		this.importer = new Importer(this);
		this.console = console;
		
		gui = new GUI(this, traceTimeRange, width, height);
		
		//read network file first
		console.print("Importing network data...");
		importer.readNetworkFile(networkFileName);
		nodes = importer.getNodes();
		links = importer.getLinks();
		gui.setNetwork(nodes,links);
		console.println("done.");
		
		if (!liveMode)
		{
			//offline mode
			console.print("Launching OFFLINE MODE...");
			
			//read file /w agent data
			console.print("Importing agent data from event file...");
			importer.readEventFile(eventFileName);
			
			//Import agent data
			agents = importer.importAgentData();
			timeSteps = importer.getTimeSteps();
			console.println("done.");
	
			//initialize GUI
			console.print("Initializing GUI...");
			console.println("done.");
	
		}
		
		extremeValues = importer.getExtremeValues();
		//Double[] extremeValues = {maxPosX, maxPosY, maxPosZ, minPosX, minPosY, minPosZ, maxTimeStep, minTimeStep};
		//                          0        1        2        3        4        5        6            7
		
		console.println(extremeValues.toString());
		
		maxPosX = (extremeValues[0] != null) ? extremeValues[0]:Double.MIN_VALUE;
		maxPosY = (extremeValues[1] != null) ? extremeValues[1]:Double.MIN_VALUE;
		minPosX = (extremeValues[3] != null) ? extremeValues[3]:Double.MAX_VALUE;
		minPosY = (extremeValues[4] != null) ? extremeValues[4]:Double.MAX_VALUE;
		
		timeSteps = new LinkedList<Double>();
		
		gui.setAgentData(agents,extremeValues,timeSteps);
		gui.setNetwork(nodes,links);
		gui.init();
		gui.setVisible(true);
		
		if (liveMode)
		{
			//live mode (agent data is coming via event stream)
			console.print("Launching LIVE MODE...");
			
			//Launches the LiveMode
			EventsManager manager = EventsUtils.createEventsManager();
			XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);
			
			manager.addHandler(this.importer); // handler must implement XYVxVyEventsHandler
			reader.parse("C:\\temp5\\events2.xml");
		}
		
		
	}
	
	public void setAnimationSpeed(Float speed)
	{
		gui.setAnimationSpeed(speed);
	}

	public void play()
	{
		if (!liveMode)
			gui.play();
		
	}

	public void pause()
	{
		gui.pause();
		
	}

	public void rewind()
	{
		if (!liveMode)
			gui.rewind();
		
	}
	
	public void updateAgentData(HashMap<String,Agent> agents)
	{
		gui.updateAgentData(agents);
	}
	
	public void updateCurrentTime(double time)
	{
		gui.updateCurrentTime(time);
	}

	/**
	 * this method is used to update agent events. it will transport new
	 * agent data to the gui and update the scene. if a timechange occurs,
	 * the whole scene is being redrawn.
	 * 
	 * @param String ID the agent ID
	 * @param Double posX position X
	 * @param Double posY position Y
	 * @param Double time time
	 */
	public void updateAgentData(String ID, Double posX, Double posY, Double time)
	{
		
		//sleep until 1 sec has elapsed
		long realTime = System.currentTimeMillis();
		long timeDiff = realTime - oldTime;
		
		if (timeDiff<=timeInterval)
		{
			try {
				Thread.sleep(timeInterval-timeDiff);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		this.oldTime = realTime;
				
		console.println("ID:" + ID + "| time:" + time + "|");
		
		//on the first run in live mode the agents hashmap is still empty
		if (agents == null)
		{
			//if (ID.equals("0")) console.println("NULL AGENT");
			agents = new HashMap<String,Agent>();
			agents.put(ID, new Agent());
		}
		
		//update agent list
		Agent currentAgent = agents.get(ID);
		if (currentAgent == null) currentAgent = new Agent(); 
		
		DataPoint dataPoint = new DataPoint(time, posX, posY);
		console.println("adding dp to agent " + ID + ":" + dataPoint.toString() + "|");
		currentAgent.addDataPoint(dataPoint);
		
		agents.put(ID, currentAgent);
		
		//on the first run the current time is not set yet
		if (currentTime.equals(Double.NaN))
		{
			currentTime = time;
			console.println("@@@@@@@@ ADD @@@@@@@");
			timeSteps.addLast(currentTime);			
		}
		else
		{
			//if a timestep occured
			if (!currentTime.equals(time))
			{
				console.println("time change occured!");
				
				//recalculate extreme values
				if (agents != null)
				{
					Iterator agentsIterator = agents.entrySet().iterator();
					while (agentsIterator.hasNext())
					{
						//Get current agent
						Map.Entry pairs = (Map.Entry) agentsIterator.next();
						Agent agent = (Agent)pairs.getValue();
						
						HashMap<Double,DataPoint> dataPoints = agent.getDataPoints();
						Iterator dataPointIterator = dataPoints.entrySet().iterator();
						
						while (dataPointIterator.hasNext())
						{
							//Get current datapoint
							Map.Entry dataPointPairs = (Map.Entry) dataPointIterator.next();
							DataPoint itDataPoint = (DataPoint)dataPointPairs.getValue();
							
							if (itDataPoint != null)
							{
								maxPosX = Math.max(itDataPoint.getPosX(), maxPosX);
								minPosX = Math.min(itDataPoint.getPosX(), minPosX);
								maxPosY = Math.max(itDataPoint.getPosY(), maxPosY);
								minPosY = Math.min(itDataPoint.getPosY(), minPosY);
							}
						}
						
					}
					
				}
				
				
				
				//if there are already more timesteps than the range of traces
				//(timesteps) to show: truncate in timestep list and agent data
				while (timeSteps.size() > traceTimeRange)
				{
					console.println("-@@@@@@@-++-----" + timeSteps.toString());
					//delete the oldest timestamp from the timestep list
					Double removedTimeStep = timeSteps.removeFirst();
					console.println("-@@@@@@@----__------" + timeSteps.toString());
					
					//remove oldest timesteps from agents
					if (agents != null)
					{
						//iterate through all agents and delete oldest timestep
						Iterator agentsIterator = agents.entrySet().iterator();
						while (agentsIterator.hasNext())
						{
							//Get current agent
							Map.Entry pairs = (Map.Entry) agentsIterator.next();
							Agent agent = (Agent)pairs.getValue();
							agent.removeDataPoint(removedTimeStep);
						}
					}
					
				}
				
				
				gui.updateExtremeValues(maxPosX, minPosX, maxPosY, minPosY);
				gui.updateView(timeSteps, agents);
				
				currentTime = time;
				timeSteps.addLast(currentTime);
				
				
			}
			
		}		
		

		
	}


}
