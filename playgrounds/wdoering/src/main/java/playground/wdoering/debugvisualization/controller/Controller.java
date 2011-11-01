package playground.wdoering.debugvisualization.controller;


import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
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

	private static final long timeInterval = 40;
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
	private Thread readerThread;
	private boolean paused = false;

	EventsManager eventsManager;
	Scenario scenario;

	public Console console;
	private Long oldTime = 0l;

	public boolean isLiveMode() {
		return this.liveMode;
	}

	public void setLiveMode(boolean liveMode) {
		this.liveMode = liveMode;
	}

	public Controller() //klasse scenario + handler übergeben
	{

	}

	public Controller(String eventFileName, String networkFileName, Console console, int traceTimeRange, boolean liveMode)
	{

		this.maxPosX = this.maxPosY = Double.MIN_VALUE;
		this.minPosX = this.minPosY = Double.MAX_VALUE;

		this.liveMode = liveMode;
		this.traceTimeRange = traceTimeRange;

		//set up importer. can surely be replaced.
		this.importer = new Importer(this);
		this.console = console;

		this.gui = new GUI(this, traceTimeRange, width, height);

		//read network file first
		console.print("Importing network data...");
		this.importer.readNetworkFile(networkFileName);
		this.nodes = this.importer.getNodes();
		this.links = this.importer.getLinks();
		this.gui.setNetwork(this.nodes,this.links);
		console.println("done.");

		if (!liveMode)
		{
			//offline mode
			console.print("Launching OFFLINE MODE...");

			//read file /w agent data
			console.print("Importing agent data from event file...");
			this.importer.readEventFile(eventFileName);

			//Import agent data
			this.agents = this.importer.importAgentData();
			this.timeSteps = this.importer.getTimeSteps();
			console.println("done.");

			//initialize GUI
			console.print("Initializing GUI...");
			console.println("done.");

		}

		this.extremeValues = this.importer.getExtremeValues();
		//Double[] extremeValues = {maxPosX, maxPosY, maxPosZ, minPosX, minPosY, minPosZ, maxTimeStep, minTimeStep};
		//                          0        1        2        3        4        5        6            7

		console.println(this.extremeValues.toString());

		this.maxPosX = (this.extremeValues[0] != null) ? this.extremeValues[0]:Double.MIN_VALUE;
		this.maxPosY = (this.extremeValues[1] != null) ? this.extremeValues[1]:Double.MIN_VALUE;
		this.minPosX = (this.extremeValues[3] != null) ? this.extremeValues[3]:Double.MAX_VALUE;
		this.minPosY = (this.extremeValues[4] != null) ? this.extremeValues[4]:Double.MAX_VALUE;

		this.timeSteps = new LinkedList<Double>();

		this.gui.setAgentData(this.agents,this.extremeValues,this.timeSteps);
		this.gui.setNetwork(this.nodes,this.links);
		this.gui.init();
		this.gui.setVisible(true);

		if (liveMode)
		{
			//live mode (agent data is coming via event stream)
			console.print("Launching LIVE MODE...");

			//Launches the LiveMode
			EventsManager manager = EventsUtils.createEventsManager();
			XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);

			manager.addHandler(this.importer); // handler must implement XYVxVyEventsHandler
			reader.parse("C:\\temp\\events2.xml");
		}


	}

	public Controller(EventsManager e, Scenario sc, Console console, Thread readerThread)
	{

		//assign console
		this.console = console;

		this.readerThread = readerThread;

		//set to live mode
		this.liveMode = true;

		//Thread importerThread = new Thread(new Importer(this,sc),"ha");


		//assign scenario and eventsmanager
		this.scenario = sc;
		this.eventsManager = e;

		//import network data via network file and
		console.print("Importing network data...");
		this.importer = new Importer(this, this.scenario, readerThread);
		this.nodes = this.importer.getNodes();
		this.links = this.importer.getLinks();

		//set determined extreme value coordinates
		this.extremeValues = this.importer.getExtremeValues();
		this.maxPosX = (this.extremeValues[0] != null) ? this.extremeValues[0]:Double.MIN_VALUE;
		this.maxPosY = (this.extremeValues[1] != null) ? this.extremeValues[1]:Double.MIN_VALUE;
		this.minPosX = (this.extremeValues[3] != null) ? this.extremeValues[3]:Double.MAX_VALUE;
		this.minPosY = (this.extremeValues[4] != null) ? this.extremeValues[4]:Double.MAX_VALUE;

		//import scenario data finished
		console.println("done.");

		//set up gui and network
		this.gui = new GUI(this, this.traceTimeRange, width, height);

		//process final mandatory display data
		this.timeSteps = new LinkedList<Double>();
		this.gui.setAgentData(this.agents,this.extremeValues,this.timeSteps);
		this.gui.setNetwork(this.nodes,this.links);
		this.gui.init();
		this.gui.setVisible(true);

		this.eventsManager.addHandler(this.importer);

		this.readerThread.start();

		//eventsManager.

		// TODO Auto-generated constructor stub
	}

	public void setAnimationSpeed(Float speed)
	{
		this.gui.setAnimationSpeed(speed);
	}

	public void play()
	{
		if (!this.liveMode)
			this.gui.play();

	}

	public void pause()
	{
		if (!this.paused)
		{
			System.out.print("suspend..");
			this.readerThread.suspend();
			System.out.println(".done.");
		}
		else
		{
			System.out.print("resume..");
			this.readerThread.resume();
			System.out.println(".done.");

		}

		this.gui.pause();
		this.paused = !this.paused;

	}

	public void rewind()
	{
		if (!this.liveMode)
			this.gui.rewind();

	}

	public void updateAgentData(HashMap<String,Agent> agents)
	{
		this.gui.updateAgentData(agents);
	}

	public void updateCurrentTime(double time)
	{
		this.gui.updateCurrentTime(time);
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

		//@TODO: capsulate truncate old data function and maybe even more
		//for a better readability



		this.console.println("ID:" + ID + "| time:" + time + "|");

		//on the first run in live mode the agents hashmap is still empty
		if (this.agents == null)
		{
			//if (ID.equals("0")) console.println("NULL AGENT");
			this.agents = new HashMap<String,Agent>();
			this.agents.put(ID, new Agent());
		}

		//update agent list
		Agent currentAgent = this.agents.get(ID);
		if (currentAgent == null) currentAgent = new Agent();

		DataPoint dataPoint = new DataPoint(time, posX, posY);
		this.console.println("adding dp to agent " + ID + ":" + dataPoint.toString() + "|");
		currentAgent.addDataPoint(dataPoint);

		this.agents.put(ID, currentAgent);

		//on the first run the current time is not set yet
		if (this.currentTime.equals(Double.NaN))
		{
			this.currentTime = time;
			this.console.println("@@@@@@@@ ADD @@@@@@@");
			this.timeSteps.addLast(this.currentTime);
		}
		else
		{
			//if a timestep occured
			if (!this.currentTime.equals(time))
			{
				//we only check this if a timestep has occured
				//sleep until 1 sec has elapsed
				long realTime = System.currentTimeMillis();
				long timeDiff = realTime - this.oldTime;

				if (timeDiff<timeInterval)
				{
					System.out.println("sleeping for" + (timeInterval-timeDiff) + " ms");
					try {
						Thread.sleep(timeInterval-timeDiff);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				this.oldTime = System.currentTimeMillis();
				this.console.println("time change occured!");

				//recalculate extreme values
				if (this.agents != null)
				{
					Iterator agentsIterator = this.agents.entrySet().iterator();
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
								this.maxPosX = Math.max(itDataPoint.getPosX(), this.maxPosX);
								this.minPosX = Math.min(itDataPoint.getPosX(), this.minPosX);
								this.maxPosY = Math.max(itDataPoint.getPosY(), this.maxPosY);
								this.minPosY = Math.min(itDataPoint.getPosY(), this.minPosY);
							}
						}

					}

				}



				//if there are already more timesteps than the range of traces
				//(timesteps) to show: truncate in timestep list and agent data
				while (this.timeSteps.size() > this.traceTimeRange)
				{
					this.console.println("-@@@@@@@-++-----" + this.timeSteps.toString());
					//delete the oldest timestamp from the timestep list
					Double removedTimeStep = this.timeSteps.removeFirst();
					this.console.println("-@@@@@@@----__------" + this.timeSteps.toString());

					//remove oldest timesteps from agents
					if (this.agents != null)
					{
						//iterate through all agents and delete oldest timestep
						Iterator agentsIterator = this.agents.entrySet().iterator();
						while (agentsIterator.hasNext())
						{
							//Get current agent
							Map.Entry pairs = (Map.Entry) agentsIterator.next();
							Agent agent = (Agent)pairs.getValue();
							agent.removeDataPoint(removedTimeStep);
						}
					}

				}


				this.gui.updateExtremeValues(this.maxPosX, this.minPosX, this.maxPosY, this.minPosY);
				this.gui.updateView(this.timeSteps, this.agents);

				this.currentTime = time;
				this.timeSteps.addLast(this.currentTime);


			}

		}



	}


}
