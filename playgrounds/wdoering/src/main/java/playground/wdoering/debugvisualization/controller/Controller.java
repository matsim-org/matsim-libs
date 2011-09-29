package playground.wdoering.debugvisualization.controller;


import java.util.ArrayList;
import java.util.HashMap;

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
	
	public Console console;
	
	public Controller(String eventFileName, String networkFileName, Console console)
	{
		//set up importer. can surely be replaced.
		this.importer = new Importer();
		this.console = console;
		
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

	


}
