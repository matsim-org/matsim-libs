package playground.wdoering.debugvisualization.controller;


import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
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

	private GUI gui;
	private Importer importer;
	private HashMap<Integer,Agent> agents;
	private HashMap<Integer,DataPoint> nodes;
	private HashMap<Integer,int[]> links;
	private Double[] extremeValues;
	private Double[] timeSteps;
	private boolean liveMode;
	
	public Console console;
	
	public Controller(String eventFileName, String networkFileName, Console console, boolean liveMode)
	{
		
		this.liveMode = liveMode;

		//set up importer. can surely be replaced.
		this.importer = new Importer(this);
		this.console = console;
		
		if (liveMode)
		{
			EventsManager manager = EventsUtils.createEventsManager();
			XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);
			
	
			manager.addHandler(this.importer); // handler muss XYVxVyEventsHandler implementieren
			reader.parse("C:\\temp5\\events2.xml");
		}
		else
		{
			
			//read file /w agent data
			importer.readEventFile(eventFileName);
			importer.readNetworkFile(networkFileName);
			
			//Import agent data
			console.print("Importing agent data...");
			agents = importer.importAgentData();
			extremeValues = importer.getExtremeValues();
			timeSteps = importer.getTimeSteps();
			nodes = importer.getNodes();
			links = importer.getLinks();
			console.println("done.");
	
			console.print("Initializing GUI...");
			gui = new GUI(this);
			console.println("done.");
	
			gui.setAgentData(agents,extremeValues,timeSteps);
			gui.setNetwork(nodes,links);
		}
		
		gui.init();
		gui.setVisible(true);
		
		
	}
	
	public void setAnimationSpeed(Float speed)
	{
		gui.setAnimationSpeed(speed);
	}

	public void play()
	{
		gui.play();
		
	}

	public void pause()
	{
		gui.pause();
		
	}

	public void rewind()
	{
		gui.rewind();
		
	}
	
	public void updateAgentData(HashMap<Integer,Agent> agents)
	{
		gui.updateAgentData(agents);
	}
	
	public void updateCurrentTime(double time)
	{
		gui.updateCurrentTime(time);
	}

	


}
