package playground.wdoering.debugvisualization.gui;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import playground.wdoering.debugvisualization.controller.AgentDataController;
import playground.wdoering.debugvisualization.controller.Console;
import playground.wdoering.debugvisualization.controller.Controller;

import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import playground.wdoering.debugvisualization.model.Scene;
import playground.wdoering.debugvisualization.model.XYVxVyAgent;
import playground.wdoering.debugvisualization.model.XYVxVyDataPoint;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PShapeSVG.Font;

//import processing.core.*;
//import processing.opengl.*;

/**
 * GL Renderer
 * 
 * Extends the processing applet.
 * Currently NOT using GL.
 * 
 * @author wdoering
 *
 */
public class P3DRenderer extends PApplet
{

	private final float agentSize = 20; //(meter / si units)
	private HashMap<String, Agent> agents;
	private HashMap<String, Agent> agentsCopy = null;
	private HashMap<Integer, DataPoint> nodes;
	private HashMap<Integer, int[]> links;
	//private LinkedList<HashMap<Integer, Agent>>;

	private final int width;
	private final int height;
	
	private int mode;
	
	private Double[] extremeValues;
	private LinkedList<Double> timeSteps;
	private boolean paused;
	private boolean rewind;
	private final boolean liveMode;

	private float factorX;
	private float factorY;
	private float i;
	private int iInt;
	private double minPosX;
	private double maxPosX;
	private double minPosY;
	private double maxPosY;
	private double currentTime;
	private boolean displayingData = false;
	private int currentFrame = 1;
	private Double avgRenderingTime = 1d;
	private Visualization visualization;
	private boolean mousePressed = false;
	private final Console console;
	private int traceTimeRange = 3;
	private LinkedList<Scene> scenes;
	private ArrayList<int[]> colors;
	private Controller controller;
	private long oldTime;
	private boolean mouseDragged;
	private Point mouseDragBeginPosition = new Point(0,0);
	
	private ArrayList<Geometry> geometries = null;
	
	private Point panOffset = new Point(0,0);
	private Point offset = new Point(0,0);
	
	private float zoomFactor = 10;
	private AgentDataController agentDataController;
	
	

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public HashMap<String, Agent> getAgents() {
		return agents;
	}

	public void setAgents(HashMap<String, Agent> agents) {
		this.agents = agents;
	}

	public HashMap<Integer, DataPoint> getNodes() {
		return nodes;
	}

	public void setNodes(HashMap<Integer, DataPoint> nodes) {
		this.nodes = nodes;
	}

	public HashMap<Integer, int[]> getLinks() {
		return links;
	}

	public void setLinks(HashMap<Integer, int[]> links) {
		this.links = links;
	}

	public LinkedList<Double> getTimeSteps() {
		return timeSteps;
	}

	public void setTimeSteps(LinkedList<Double> timeSteps) {
		this.timeSteps = timeSteps;
	}

	public float getFactorX() {
		return factorX;
	}

	public void setFactorX(float factorX) {
		this.factorX = factorX;
	}

	public float getFactorY() {
		return factorY;
	}

	public void setFactorY(float factorY) {
		this.factorY = factorY;
	}

	public double getMinPosX() {
		return minPosX;
	}

	public void setMinPosX(double minPosX) {
		this.minPosX = minPosX;
	}

	public double getMaxPosX() {
		return maxPosX;
	}

	public void setMaxPosX(double maxPosX) {
		this.maxPosX = maxPosX;
	}

	public double getMinPosY() {
		return minPosY;
	}

	public void setMinPosY(double minPosY) {
		this.minPosY = minPosY;
	}

	public double getMaxPosY() {
		return maxPosY;
	}

	public void setMaxPosY(double maxPosY) {
		this.maxPosY = maxPosY;
	}

	public double getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(double currentTime) {
		this.currentTime = currentTime;
	}

	public boolean isMousePressed() {
		return mousePressed;
	}

	public void setMousePressed(boolean mousePressed) {
		this.mousePressed = mousePressed;
	}

	public ArrayList<int[]> getColors() {
		return colors;
	}

	public void setColors(ArrayList<int[]> colors) {
		this.colors = colors;
	}

	public Controller getController() {
		return controller;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public float getAgentSize() {
		return agentSize;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Double[] getExtremeValues() {
		return extremeValues;
	}

	public boolean isLiveMode() {
		return liveMode;
	}

	public Console getConsole() {
		return console;
	}

	public int getTraceTimeRange() {
		return this.traceTimeRange;
	}

	public void setTraceTimeRange(int traceTimeRange) {
		this.traceTimeRange = traceTimeRange;
	}

	public boolean isPaused()
	{
		return this.paused;
	}

	public void setPaused(boolean paused)
	{
		this.paused = paused;
	}

	public P3DRenderer(Controller controller, int traceTimeRange, int width, int height, int visualizationMode)
	{
		
		if (visualizationMode == controller.VIS_XYVXVY)
			this.mode = controller.VIS_XYVXVY;
		
		this.traceTimeRange = traceTimeRange;
		
		this.controller = controller;
		
		this.liveMode = controller.isLiveMode();
		this.console = controller.console;

		this.width = width;
		this.height = height;

		this.agentDataController = controller.getAgentDataController();
		
		this.geometries = controller.getGeometries();

		setAgentColors(2500);

	}

	public void setExtremeValues(Double[] extremeValues)
	{
		//Double[] extremeValues = {maxPosX, maxPosY, maxPosZ, minPosX, minPosY, minPosZ, maxTimeStep, minTimeStep};
		//                          0        1        2        3        4        5        6            7

		this.extremeValues = extremeValues;

		this.maxPosX = extremeValues[0];
		this.maxPosY = extremeValues[1];
		this.minPosX = extremeValues[3];
		this.minPosY = extremeValues[4];

	}

	/**
	 * sets the color model to use
	 * 
	 * @param agentCount number of agents to colorize
	 */
	public void setAgentColors(int agentCount)
	{
		
		int agentColorCount;
		
		if (agentCount > 255)
			agentColorCount = 255;
		else
			agentColorCount = agentCount;
		
		this.colors = new ArrayList<int[]>();
		for (int j = 0; j < agentCount; j++)
		{
			
			//determine color
			int[] color = {(int)((((float)agentColorCount-(float)j)/agentColorCount)*255),
						   (int)((double)(((float)agentColorCount-(float)j)/agentColorCount)*255),
						   (int)(255-(double)(((float)agentColorCount-(float)j)/agentColorCount)*255)};

			//console.println(color[0] + "|" + color[1] + "|" + color[2]);

			//add color to the color array
			this.colors.add(color);
		}

		System.out.println("color count:" + this.colors.size());


		/*
		//set up color matrix to display agents
		this.colors = new ArrayList<int[]>();

		//give each agent a different (preattentive) color. If there are more then 10, generate the colors.
		if (agentCount>10)
		{

			for (int j = 0; j < agentCount; j++)
			{
				//determine color
				int[] color = {(int)((((float)agentCount-(float)j)/(float)agentCount)*255),
						       (int)((double)(((float)agentCount-(float)j)/(float)agentCount)*255),
						       (int)(255-(double)(((float)agentCount-(float)j)/(float)agentCount)*255)};

				//console.println(color[0] + "|" + color[1] + "|" + color[2]);

				//add color to the color array
				colors.add(color);
			}
		}
		else //otherwise use these 10 optimized colors (src: Colin Ware)
		{

			String colorsRGB[] = {  "#FB253C",
									"#F2F319",
									"#3BCF49",
									"#413FB0",
									"#B82828",
									"#AC0297",
									"#FCA147",
									"#98A192",
									"#72D2D2",
									"#FC82AF" };

			for (int j= 0; j < 10; j++)
			{
				int[] color = { Integer.parseInt(colorsRGB[j].substring(1,3),16),
							    Integer.parseInt(colorsRGB[j].substring(3,5),16),
							    Integer.parseInt(colorsRGB[j].substring(5,7),16)};

				colors.add(color);
			}
		}
		 */

	}


	/**
	 * Sets extreme values
	 * 
	 * @param agents
	 * @param extremeValues
	 * @param timeSteps
	 */
	public void setAgentData(HashMap<String, Agent> agents, Double[] extremeValues, LinkedList<Double> timeSteps)
	{
		//pass agents through
		this.agents = agents;

		//get extreme values, store them locally
		setExtremeValues(extremeValues);

		//pass time step data through
		this.timeSteps = timeSteps;

		//set up color matrix to display agents
		//this.colors = new ArrayList<int[]>();

		//		if (agents !=null)
		//		{
		//			//give each agent a different (preattentive) color. If there are more then 10, generate the colors.
		//			if (agents.size()>10)
		//			{
		//
		//				for (int j = 0; j < agents.size(); j++)
		//				{
		//					//determine color
		//					int[] color = {(int)((((float)agents.size()-(float)j)/(float)agents.size())*255),
		//							       (int)((double)(((float)agents.size()-(float)j)/(float)agents.size())*255),
		//							       (int)(255-(double)(((float)agents.size()-(float)j)/(float)agents.size())*255)};
		//
		//					//console.println(color[0] + "|" + color[1] + "|" + color[2]);
		//
		//					//add color to the color array
		//					colors.add(color);
		//				}
		//			}
		//			else //otherwise use these 10 optimized colors (src: Colin Ware)
		//			{
		//
		//				String colorsRGB[] = {  "#FB253C",
		//										"#F2F319",
		//										"#3BCF49",
		//										"#413FB0",
		//										"#B82828",
		//										"#AC0297",
		//										"#FCA147",
		//										"#98A192",
		//										"#72D2D2",
		//										"#FC82AF" };
		//
		//				for (int j= 0; j < agents.size(); j++)
		//				{
		//					int[] color = { Integer.parseInt(colorsRGB[j].substring(1,3),16),
		//								    Integer.parseInt(colorsRGB[j].substring(3,5),16),
		//								    Integer.parseInt(colorsRGB[j].substring(5,7),16)};
		//
		//					colors.add(color);
		//				}
		//			}
		//		}

	}

	public void setTimeRange(Double from, Double to)
	{
		//not implemented yet

	}

	float c = 1;

	//not implemented yet
	PImage b = loadImage("http://upload.wikimedia.org/wikipedia/commons/thumb/1/10/LaBelle_Blueprint.jpg/250px-LaBelle_Blueprint.jpg");
	private XYVxVyAgent toolTippedAgent;
	private float toolTippedAgentPosX;
	private float toolTippedAgentPosY;
	private String toolTippedAgentID;
	private XYVxVyDataPoint toolTippedAgentDataPoint;
	private String toolTipAgentText;

	@Override
	public void setup()
	{
		
		 addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
			    public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
			      mouseWheel(evt.getWheelRotation());
			  }}); 		
		
		size(this.width, this.height, P3D);
		frameRate(60);

		noStroke();

		this.i = 0;
		this.paused=false;
		this.rewind=false;

		PFont font;
		//System.out.println(System.getProperty("user.dir"));

		//FIXME fonts must not be stored in src folder
		//		font = loadFont(System.getProperty("user.dir") + "\\src\\main\\java\\playground\\wdoering\\debugvisualization\\fonts\\LucidaConsole-12.vlw");
		//		textFont(font);


	}
	
	void mouseWheel(int delta)
	{
		
		  if (delta>0)
		  {
			  if (zoomFactor>10)
				  zoomFactor+=0.5f;
			  else
				  zoomFactor+=0.5f;
			  
			  
		  }
		  else
		  {
			  if (zoomFactor<=10)
				  zoomFactor-=0.5f;
			  else
				  zoomFactor-=0.5f;
		  }
		  
		  //offset.x = (int)(offset.x + (mouseX-width/2) * (zoomFactor/10.02f));
		 // offset.y = (int)(offset.y + (mouseY-height/2) * (zoomFactor/10.02f));
			  
			  
	}	
	
	@Override
	public void mouseDragged()
	{
		if (!this.mouseDragged)
		{
			this.mouseDragBeginPosition = new Point(mouseX, mouseY);
			this.mouseDragged = true;
		}
	}

	@Override
	public void mousePressed()
	{
		if(!this.mousePressed)
			this.mousePressed = true;
		
	}

	@Override
	public void mouseReleased()
	{
		this.mousePressed = false;
		this.mouseDragged = false;
		
		if ((panOffset.x!=0) || (panOffset.y!=0))
		{
				offset.x = panOffset.x = offset.x + mouseX - mouseDragBeginPosition.x;
				offset.y = panOffset.y = offset.y + mouseY - mouseDragBeginPosition.y;
				//offset.y = panOffset.y;
				panOffset.x = panOffset.y = 0;
		}
		
	}
	
	

	/**
	 * draw function. calculating proportions.
	 * iterating through agent data, drawing agents,
	 * agent trajectories and network.
	 * 
	 */
	@Override
	public void draw()
	{
		
		if ((this.extremeValues != null) && (this.factorX == 0.0f))
		{
			//get max values
			float maxWidth = (float)(this.extremeValues[0]-this.extremeValues[3]);
			float maxHeight = (float)(this.extremeValues[1]-this.extremeValues[4]);

			float longerSide = Math.max(maxWidth,  maxHeight);
			float shorterSide = Math.min(maxWidth, maxHeight);
			
			if (maxWidth == longerSide)
				offset.y = (int)(longerSide - shorterSide) / this.height;
			else
				offset.x = (int)(longerSide - shorterSide) / this.width;
			
			//refactor min/max values of event+network file
			this.factorX = longerSide / this.width;
			this.factorY = longerSide / this.height;

			//console.println(((this.maxPosX-this.minPosX) /factorX));
		}		
		
		//mouse drag
		if (mouseDragged)
		{
			if ((mouseDragBeginPosition.x!=0) && (mouseDragBeginPosition.x!=0))
			{
				panOffset.x = mouseX - mouseDragBeginPosition.x;
				panOffset.y = mouseY - mouseDragBeginPosition.y;
				
			}
		}
		
		
		if (currentFrame<Integer.MAX_VALUE)
			currentFrame++;
		else
			currentFrame=1;
		
		double timeMeas = System.currentTimeMillis();
		background(33);
		fill(0, 0, 0);
		//visualization.draw();
		
		
		//FIRST  ----------------------------------------
		//draw network
		drawNetwork();
		
		//draw geometries (walls etc)
		drawGeometries();
		
		//SECOND ----------------------------------------
		//draw agents
		if (this.liveMode)
		{

			drawAgents();

		}
		else
		{
			drawOfflineAgents();
		}
		
		double timeMeasDiff= System.currentTimeMillis()-timeMeas;
		avgRenderingTime += timeMeasDiff;
		


	}

	private void drawGeometries()
	{
		//Draw Network
				if ((this.geometries!=null))
				{
					//Stroke color
					stroke(100,255,0,250);

					//Iterate through all geometries + coordinates
					for (Geometry geo : geometries)
					{
						
						Coordinate [] coordinates = geo.getCoordinates();
						if (coordinates.length>0)
						{
							Coordinate lastCoordinate = null;
							
							for (int i = 0; i < coordinates.length; i++)
							{
								if (lastCoordinate != null)
								{
									//draw a line (from - to - datapoint)
									line((float)((coordinates[i].x-this.minPosX)/this.factorX*(this.zoomFactor/10f))+panOffset.x+offset.x,
											(float)(height-((coordinates[i].y-this.minPosY))/this.factorY*(this.zoomFactor/10f))+panOffset.y+offset.y,
											(float)((lastCoordinate.x-this.minPosX)/this.factorX*(this.zoomFactor/10.02f))+panOffset.x+offset.x,
											(float)(height-((lastCoordinate.y-this.minPosY))/this.factorY*(this.zoomFactor/10.02f))+panOffset.y+offset.y);
								}
								lastCoordinate = coordinates[i];
							}
						}
					}
					noStroke();
				}
		
	}

	private void drawAgents()
	{

		



		if (this.mousePressed)
		{
			this.c=this.c+2.1f;
			//scale(1/c, 1/c, c);
			//background(33,255,127+(c%127));

		}
		//background(200);

		//			smooth();
		//			strokeWeight(4);

		textMode(SCREEN);

		//noStroke();

		//Stroke color
		//			smooth();
		//			strokeWeight(4);
		//stroke(100,0,100,100);


		//console.println(timeSteps);
		
		//agents from synchronized agent data controller
		try { agentsCopy = (HashMap<String, Agent>)agentDataController.getAgents().clone(); } catch (Exception e) {}

		
		if ((this.agentsCopy != null) && (this.timeSteps != null))
		{
			if (this.timeSteps.size()>0)
			{

				//to determine whether a tooltip has already been drawn or not 
				boolean drawTooltip = false;

				//Iterate through all agents and display the current data point + traces
				
				while (displayingData)
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				displayingData = true;
				
				
				if (agentsCopy!=null)
				{
					
					Iterator agentsIterator = agentsCopy.entrySet().iterator();
					int agentCount = 0;
					
					//While there are still agents in the agents array
					while (agentsIterator.hasNext())
					{
						Map.Entry pairs = null;
						//Get current agent
						pairs = (Map.Entry) agentsIterator.next();
						
						XYVxVyAgent currentAgent = (XYVxVyAgent)pairs.getValue();
						String currentAgentID = (String)pairs.getKey();
	
						HashMap<Double,DataPoint> dataPoints = currentAgent.getDataPoints();
	
						//check if there are any data points for the current agent
						if ((dataPoints != null) && (dataPoints.size() > 0))
						{
							//pick preattentive agent color
							int[] agentColor = new int[3];
	
							//							if (colors.size()<=agentCount)
							//								setAgentColors(agentCount+1);
	
							agentColor = this.colors.get(agentCount);
							strokeWeight(4);   // Thicker
	
							//draw node trajectories if there is more then one datapoint for the current agent
							if (dataPoints.size() > 1)
							{
								//number of lines (trajectories) to draw (between 2 and traceTimeRange)
								int traceDisplayCount = Math.min(this.traceTimeRange, dataPoints.size());
	
								//loop through the datapoints with the corresponding timesteps
								for (int timeStep = 0; timeStep < traceDisplayCount-2; timeStep++)
								{
	
									this.console.println("tp size: " + traceDisplayCount + " | dp size:" + dataPoints.size() + "| current timestep: " + timeStep + "| timesteps: " + this.timeSteps.size());
	
									//extract current and next datapoint (to draw a trajectory line)
									DataPoint currentDataPoint = dataPoints.get(this.timeSteps.get(timeStep));
									DataPoint nextDataPoint = dataPoints.get(this.timeSteps.get(timeStep+1));
	
									if ((currentDataPoint != null )&&(nextDataPoint != null))
									{
										//pick line color and make far away trajectories more transparent
										float jFloat = timeStep;
										float iFloat = traceDisplayCount;
										stroke(agentColor[0], agentColor[1],agentColor[2],255-(int)(255f*((iFloat+1f-jFloat)/iFloat)));
	
										this.console.println("@@@ cdp:" + currentDataPoint.toString());
										this.console.println("@@@ x:"+ currentDataPoint.getPosX());
										this.console.println("@@@ mipX:"+ this.minPosX);
										this.console.println("@@@ fX:"+ this.factorX);
	
										//draw line
										line((float)(((currentDataPoint.getPosX() - this.minPosX) / this.factorX*(this.zoomFactor/10.02f))+panOffset.x+offset.x),
												(float)((height-(currentDataPoint.getPosY() - this.minPosY) / this.factorY*(this.zoomFactor/10.02f))+panOffset.y+offset.y),
												(float)(((nextDataPoint.getPosX()    - this.minPosX) / this.factorX*(this.zoomFactor/10.02f))+panOffset.x+offset.x),
												(float)((height-(nextDataPoint.getPosY()    - this.minPosY) / this.factorY*(this.zoomFactor/10.02f))+panOffset.y+offset.y));
									}
	
								}
	
	
	
	
							}
							noStroke();
	
							XYVxVyDataPoint lastDataPoint = (XYVxVyDataPoint)dataPoints.get(this.currentTime);
							//console.println("current agent: " + currentAgent.get);
							//							console.println("@________@________@: " + timeSteps.toString());
							//							console.println("TS GET LAST " + timeSteps.getLast());
							//							console.println("AVAILABLE DP: " + dataPoints.toString());
							//							console.println("| " + lastDataPoint.toString());
	
	
							if (lastDataPoint != null)
							{
								//draw agent
								
								//calculate relative position
								float posX = (float)((lastDataPoint.getPosX()-this.minPosX) / this.factorX*(this.zoomFactor/10.02f))+panOffset.x+offset.x;
								float posY = (float)(this.height-(((lastDataPoint.getPosY())-this.minPosY)) / this.factorY*(this.zoomFactor/10.02f))+panOffset.y+offset.y;
								
								
								this.console.println("********* MINPOS X: "+ this.minPosX +" | FACT X: " + this.factorX + "***************");
								this.console.println("********* MINPOS Y: "+ this.minPosY +" | FACT Y: " + this.factorY + "***************");
								this.console.println("********* DRAW: posX:" + posX + "| posY: " + posY + "***************");
	
								//stroke
								//								SMOOTH();
								//								STROKEWEIGHT(4);
								//								STROKE(0,0,0);
	
								//noStroke();
	
								int halfAgentSize = (int) this.agentSize / 2;
	
								if     ((!drawTooltip)
									&& (this.mouseX < posX+halfAgentSize) && (this.mouseX > posX-halfAgentSize)
									&& (this.mouseY < posY+halfAgentSize) && (this.mouseY > posY-halfAgentSize))
								{
									
									//calculate v
									float v = (float)Math.sqrt((lastDataPoint.getvX()*lastDataPoint.getvX()
											+ lastDataPoint.getvY()*lastDataPoint.getvY()));
									
									//create tooltip string, save x + y position
									toolTipAgentText = "Agent " +  currentAgentID  + " | V: " + v + "\ncurrent link: " + currentAgent.getCurrentLinkID();
									
									//draw current agent white
									fill(255, 255, 255,255);
									
									//tooltipped agent found
									drawTooltip = true;
								}
								else
									fill(color(agentColor[0], agentColor[1],agentColor[2],255));
	
								ellipse (posX, posY, this.agentSize, this.agentSize);
	
	
							}
							else
							{
								//this.console.println("agent " + currentAgentID + " missing at " + this.currentTime + " (" + this.timeSteps.getLast() + ") :" + dataPoints.toString());
							}
	
	
	
	
						}
						agentCount++;
					}
					agentsCopy.clear();
					
					//draw tooltip
					if (drawTooltip)
					{
						//draw white box
						fill(255, 255, 255, 190);
						rect(mouseX+(this.agentSize/2), mouseY+(this.agentSize/2), 180, 80);
						
						//draw text
						fill(0, 0, 0, 255);
						text(toolTipAgentText,mouseX+(this.agentSize/2)+6,mouseY+(this.agentSize*1.5f));
					}
					
				}
			}
			
			displayingData = false;
		}

		
	}

	private void drawOfflineAgents()
	{
		
		if (this.extremeValues!=null)
		{

			if (!isPaused())
			{

				if (this.factorX == 0.0f)
				{
					//get max values
					float maxWidth = (float)(this.extremeValues[0]-this.extremeValues[3]);
					float maxHeight = (float)(this.extremeValues[1]-this.extremeValues[4]);

					//refactor min/max values of event+network file
					this.factorX = maxWidth / this.width;
					this.factorY = maxHeight / this.height;

					//console.println(((this.maxPosX-this.minPosX) /factorX));
				}


				background(0);

				if (!isPaused())
				{
					if ((!this.rewind) && (this.i < this.timeSteps.size() -1))
						this.i+=0.1;
					else if ((this.rewind) && (this.i > 0))
						this.i-=0.1;
				}

				this.iInt = (int)(Math.floor(this.i));


				//Draw Agents and trajectories
				int currentAgent = 0;
				if (this.agents != null)
				{

					//Iterate through all agents and display the current data point + traces
					Iterator agentsIterator = this.agents.entrySet().iterator();

					while (agentsIterator.hasNext())
					{
						//Get current agent
						Map.Entry pairs = (Map.Entry) agentsIterator.next();
						Agent agent = (Agent)pairs.getValue();
						HashMap<Double,DataPoint> dataPoints = agent.getDataPoints();

						//Motion tween between two datapoints (-> time steps)
						int tweenCount = 0;

						//pick preattentive agent color
						int[] agentColor = new int[3];
						agentColor = this.colors.get(currentAgent);

						//first check if there is any datapoint stored to the current time step
						if (dataPoints.get(this.timeSteps.get(this.iInt)) != null)
						{
							//get the datapoint
							DataPoint currentDataPoint = (XYVxVyDataPoint)dataPoints.get((this.timeSteps.get(this.iInt)));

							float posX;
							float posY;
							
							if (currentDataPoint instanceof XYVxVyDataPoint)
								System.out.println("halelujah");

							//calculate tweened x/y position
							if ((tweenCount<dataPoints.size()-2))
							{
								DataPoint nextDataPoint = dataPoints.get((this.timeSteps.get(this.iInt+1)));

								if (nextDataPoint==null)
									nextDataPoint = currentDataPoint;

								posX = (float)(        (  ((currentDataPoint.getPosX()-this.minPosX)*(1-(this.i-Math.floor(this.i))))
										+  ((nextDataPoint.getPosX()-this.minPosX)*(this.i-Math.floor(this.i)))        )
										/ this.factorX);

								posY = (float)(( (  	(currentDataPoint.getPosY()-this.minPosY)*(1-(this.i-Math.floor(this.i))))
										+  ((nextDataPoint.getPosY()-this.minPosY)*(this.i-Math.floor(this.i)))        )
										/ this.factorY);


							}
							else
							{
								posX = (float)((currentDataPoint.getPosX()-this.minPosX) / this.factorX);
								posY = (float)((currentDataPoint.getPosY()-this.minPosY) / this.factorY);
							}

							//draw trajectories
							for (int j = 1; j <= this.iInt; j++)
							{

								if ( ((dataPoints.get(this.timeSteps.get(j)) != null)) && ((dataPoints.get((this.timeSteps.get(j-1))) != null)) )
								{
									DataPoint trajectoryDataPoint1 = dataPoints.get(this.timeSteps.get(j));
									DataPoint trajectoryDataPoint2 = dataPoints.get(this.timeSteps.get(j-1));

									smooth();
									strokeWeight(4);

									//make far away trajectories more transparent
									float jFloat = j;
									float iFloat = this.iInt;
									stroke(agentColor[0], agentColor[1],agentColor[2],255-(int)(255f*((iFloat+1f-jFloat)/iFloat)));

									line((float)((trajectoryDataPoint1.getPosX()-this.minPosX)/this.factorX),
											(float)((trajectoryDataPoint1.getPosY()-this.minPosY)/this.factorY),
											(float)((trajectoryDataPoint2.getPosX()-this.minPosX)/this.factorX),
											(float)((trajectoryDataPoint2.getPosY()-this.minPosY)/this.factorY));

								}

							}

							//line from last position to tweened agent position
							line((float) ((currentDataPoint.getPosX() - this.minPosX) / this.factorX),
									(float) ((currentDataPoint.getPosY() - this.minPosY) / this.factorY),
									posX, posY);

							noStroke();

							//increment tween
							tweenCount++;

							//draw agent
							fill(color(agentColor[0], agentColor[1],agentColor[2],255));
							ellipse (posX, posY, this.agentSize, this.agentSize);

						}
						currentAgent++;

					}

				}



			}
		}
		
	}

	private void drawNetwork()
	{
		//Draw Network
		if ((this.nodes!=null) && (this.links!=null))
		{
			//Stroke color
			stroke(100,0,100,100);

			//Iterate through all nodes and the draw links
			for (Object element : this.links.values()) {
				int[] fromTo = (int[]) element;

				DataPoint fromDataPoint = this.nodes.get(fromTo[0]);
				DataPoint ToDataPoint = this.nodes.get(fromTo[1]);

				//draw a line (from - to - datapoint)
				line((float)((fromDataPoint.getPosX()-this.minPosX)/this.factorX*(this.zoomFactor/10f))+panOffset.x+offset.x,
						(float)(height-((fromDataPoint.getPosY()-this.minPosY))/this.factorY*(this.zoomFactor/10f))+panOffset.y+offset.y,
						(float)((ToDataPoint.getPosX()-this.minPosX)/this.factorX*(this.zoomFactor/10.02f))+panOffset.x+offset.x,
						(float)(height-((ToDataPoint.getPosY()-this.minPosY))/this.factorY*(this.zoomFactor/10.02f))+panOffset.y+offset.y);


			}
			noStroke();
		}
		
	}

	public void setPositionRange(Point max, Point min)
	{
		// TODO Auto-generated method stub

	}

	public void setAnimationSpeed(Float speed)
	{
		// TODO Auto-generated method stub
		//c = 100;
		// c = speed;

	}

	public void playScenario()
	{
		if (!this.rewind)
			this.i = 0;
		else
			this.i = this.timeSteps.size()-1;

	}

	public void togglePause()
	{
		this.paused = !this.paused;

	}

	public void rewindScenario()
	{
		this.rewind = !this.rewind;

	}

	public void setNetwork(HashMap<Integer, DataPoint> nodes, HashMap<Integer, int[]> links)
	{
		this.nodes = nodes;
		this.links = links;

	}

	public void updateAgentData(HashMap<String, Agent> agents)
	{
		this.agents = agents;

	}

	public void updateCurrentTime(double time)
	{
		this.currentTime = time;
		//this.t

	}

	public void updateAgentData(String ID, Double X, Double Y, Double time)
	{

		// TODO Auto-generated method stub

	}

	public void updateSceneView(LinkedList<Scene> scenes)
	{
		this.scenes = scenes;

	}

	public void updateView(LinkedList<Double> timeSteps, HashMap<String, Agent> agents)
	{
		//setAgentColors(agents.size());
		this.agents = agents;
		this.timeSteps = timeSteps;
		this.currentTime = timeSteps.getLast();
		this.console.println("update view at " + this.currentTime + "!");
	}

	/**
	 * update extreme values (only spatial extremes)
	 * 
	 * @param maxPosX
	 * @param minPosX
	 * @param maxPosY
	 * @param minPosY
	 */
	public void updateExtremeValues(Double maxPosX, Double minPosX, Double maxPosY, Double minPosY)
	{
		Double[] extremeValues = {maxPosX, maxPosY, this.extremeValues[2], minPosX, minPosY, this.extremeValues[5],
				this.extremeValues[6], this.extremeValues[7]};
		setExtremeValues(extremeValues);

	}
	

}
