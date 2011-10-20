package playground.wdoering.debugvisualization.gui;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import playground.wdoering.debugvisualization.controller.Console;

import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import playground.wdoering.debugvisualization.model.Scene;
import processing.core.PApplet;
import processing.core.PImage;

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

	private float agentSize = 20; //(meter / si units)
	private HashMap<String, Agent> agents;
	private HashMap<Integer, DataPoint> nodes;
	private HashMap<Integer, int[]> links;
	//private LinkedList<HashMap<Integer, Agent>>;
	
	
	private Double[] extremeValues;
	private LinkedList<Double> timeSteps;
	private boolean paused;
	private boolean rewind;
	private boolean liveMode;
	
	private float factorX;
	private float factorY;
	private float i;
	private int iInt;
	private double minPosX; 
	private double maxPosX; 
	private double minPosY; 
	private double maxPosY; 
	private double currentTime;
	
	private Console console;
	
	private int traceTimeRange = 3;
	
	private LinkedList<Scene> scenes;
	
	private ArrayList<int[]> colors;
	
	public int getTraceTimeRange() {
		return traceTimeRange;
	}

	public void setTraceTimeRange(int traceTimeRange) {
		this.traceTimeRange = traceTimeRange;
	}

	public boolean isPaused()
	{
		return paused;
	}

	public void setPaused(boolean paused)
	{
		this.paused = paused;
	}
	
	public P3DRenderer(boolean liveMode, int traceTimeRange, Console console)
	{
		this.traceTimeRange = traceTimeRange;
		this.liveMode = liveMode;
		this.console = console;
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
		this.colors = new ArrayList<int[]>();
		
		if (agents !=null)
		{
			//give each agent a different (preattentive) color. If there are more then 10, generate the colors.  
			if (agents.size()>10)
			{
			
				for (int j = 0; j < agents.size(); j++)
				{
					//determine color
					int[] color = {(int)((((float)agents.size()-(float)j)/(float)agents.size())*255),
							       (int)((double)(((float)agents.size()-(float)j)/(float)agents.size())*255),
							       (int)(255-(double)(((float)agents.size()-(float)j)/(float)agents.size())*255)};
					
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
				
				for (int j= 0; j < agents.size(); j++)
				{
					int[] color = { Integer.parseInt(colorsRGB[j].substring(1,3),16),
								    Integer.parseInt(colorsRGB[j].substring(3,5),16),
								    Integer.parseInt(colorsRGB[j].substring(5,7),16)};
					
					colors.add(color);
				}
			}
		}

	}

	public void setTimeRange(Double from, Double to)
	{
		//not implemented yet

	}

	float c;

	//not implemented yet
	PImage b = loadImage("C:\\temp5\\Erdgeschoss.jpg");
	private long oldTime;

	public void setup()
	{
		size(768, 768, P3D);
		frameRate(30);

		noStroke();
		
		this.i = 0;		
		paused=false;
		rewind=false;
		
	}

	/**
	 * draw function. calculating proportions.
	 * iterating through agent data, drawing agents,
	 * agent trajectories and network.
	 * 
	 */
	public void draw()
	{
		


		
		if (liveMode)
		{
			if ((extremeValues != null) && (factorX == 0.0f))
			{
				//get max values
				float maxWidth = (float)(extremeValues[0]-extremeValues[3]);
				float maxHeight = (float)(extremeValues[1]-extremeValues[4]);
								
				//refactor min/max values of event+network file
				this.factorX = maxWidth / this.width;
				this.factorY = maxHeight / this.height;
				
				//console.println(((this.maxPosX-this.minPosX) /factorX));
			}
			
			
			//Stroke color
			stroke(100,0,100,100);
			background(127);
						
			noStroke();
			
			int sceneCount = 0;
			boolean drawAgent = false;
			
			//console.println(timeSteps);
			
			if ((agents != null) && (timeSteps != null))
			{
				if (timeSteps.size()>0)
				{
					
					//console.println("----- + + + + ----- " + timeSteps.size() + "| agents:" + agents.toString());
				
					//Iterate through all agents and display the current data point + traces
					Iterator agentsIterator = agents.entrySet().iterator();
					int agentCount = 0;
					
					//While there are still agents in the agents array
					while (agentsIterator.hasNext())
					{
						
						//Get current agent
						Map.Entry pairs = (Map.Entry) agentsIterator.next();
						Agent currentAgent = (Agent)pairs.getValue();
						HashMap<Double,DataPoint> dataPoints = currentAgent.getDataPoints();
						
						//check if there are any data points for the current agent
						if ((dataPoints != null) && (dataPoints.size() > 0))
						{
							//pick preattentive agent color
							int[] agentColor = colors.get(agentCount);
								
							//draw node trajectories if there is more then one datapoint for the current agent
							if (dataPoints.size() > 1)
							{
								//number of lines (trajectories) to draw (between 2 and traceTimeRange)
								int traceDisplayCount = Math.min(traceTimeRange, dataPoints.size());
								
									//loop through the datapoints with the corresponding timesteps
									for (int timeStep = 0; timeStep < traceDisplayCount-1; timeStep++)
									{
										
										console.println("tp size: " + traceDisplayCount + " | dp size:" + dataPoints.size() + "| current timestep: " + timeStep + "| timesteps: " + timeSteps.size());
										
										//extract current and next datapoint (to draw a trajectory line)
										DataPoint currentDataPoint = dataPoints.get(timeSteps.get(timeStep));
										DataPoint nextDataPoint = dataPoints.get(timeSteps.get(timeStep+1));
										
										if ((currentDataPoint != null )&&(nextDataPoint != null))
										{
											//pick line color and make far away trajectories more transparent
											float jFloat = (float)timeStep;
											float iFloat = (float)traceDisplayCount;
											stroke(agentColor[0], agentColor[1],agentColor[2],255-(int)(255f*((iFloat+1f-jFloat)/iFloat)));
											
											console.println("@@@ cdp:" + currentDataPoint.toString());
											console.println("@@@ x:"+ currentDataPoint.getPosX());
											console.println("@@@ mipX:"+ minPosX);
											console.println("@@@ fX:"+ factorX);
											
											//draw line
											line((float)((currentDataPoint.getPosX() - minPosX) / factorX),
												 (float)((currentDataPoint.getPosY() - minPosY) / factorY),
												 (float)((nextDataPoint.getPosX()    - minPosX) / factorX),
												 (float)((nextDataPoint.getPosY()    - minPosY) / factorY));		
										}
									
									}
								
								
	
								
							}
							
							DataPoint lastDataPoint = dataPoints.get(timeSteps.getLast());
							//console.println("current agent: " + currentAgent.get);
//							console.println("@________@________@: " + timeSteps.toString());
//							console.println("TS GET LAST " + timeSteps.getLast());
//							console.println("AVAILABLE DP: " + dataPoints.toString());
//							console.println("| " + lastDataPoint.toString());
							
							
							if (lastDataPoint != null)
							{
								//draw agent
								fill(color(agentColor[0], agentColor[1],agentColor[2],255));
								
								float posX = (float)((lastDataPoint.getPosX()-minPosX) / factorX);
								float posY = (float)((lastDataPoint.getPosY()-minPosY) / factorY);
								console.println("********* MINPOS X: "+ minPosX +" | FACT X: " + factorX + "***************");
								console.println("********* MINPOS Y: "+ minPosY +" | FACT Y: " + factorY + "***************");
								console.println("********* DRAW: posX:" + posX + "| posY: " + posY + "***************");
								ellipse (posX, posY, agentSize, agentSize);
							}
							
							
							
							
						}
						agentCount++;
					}
				
				}
			}
			
			
		}
		else
		{
			
			if (extremeValues!=null)
			{
			
				if (!isPaused())
				{
			
					if (factorX == 0.0f)
					{
						//get max values
						float maxWidth = (float)(extremeValues[0]-extremeValues[3]);
						float maxHeight = (float)(extremeValues[1]-extremeValues[4]);
										
						//refactor min/max values of event+network file
						this.factorX = maxWidth / this.width;
						this.factorY = maxHeight / this.height;
						
						//console.println(((this.maxPosX-this.minPosX) /factorX));
					}
					
					
					background(0);
					
					if (!isPaused())
					{
						if ((!rewind) && (i < timeSteps.size() -1))
							i+=0.1;
						else if ((rewind) && (i > 0))
							i-=0.1;
					}
					
					iInt = (int)(Math.floor(i));
		
					//Draw Network
					if ((nodes!=null) && (links!=null))
					{
						//Stroke color
						stroke(100,0,100,100);
						
						//Iterate through all nodes and the draw links
						for (Iterator linksIterator = links.values().iterator(); linksIterator.hasNext();)
						{
							int[] fromTo = (int[]) linksIterator.next();
							
							DataPoint fromDataPoint = nodes.get(fromTo[0]);
							DataPoint ToDataPoint = nodes.get(fromTo[1]);
							
							//draw a line (from - to - datapoint)
							line((float)((fromDataPoint.getPosX()-minPosX)/factorX),
								(float)((fromDataPoint.getPosY()-minPosY)/factorY),
								(float)((ToDataPoint.getPosX()-minPosX)/factorX),
								(float)((ToDataPoint.getPosY()-minPosY)/factorY));
								
							
						}
						noStroke();
					}
					
					//Draw Agents and trajectories
					int currentAgent = 0;
					if (agents != null)
					{
						
						//Iterate through all agents and display the current data point + traces
						Iterator agentsIterator = agents.entrySet().iterator();
						
						while (agentsIterator.hasNext())
						{
							//Get current agent
							Map.Entry pairs = (Map.Entry) agentsIterator.next();
							Agent agent = (Agent)pairs.getValue();
							HashMap<Double,DataPoint> dataPoints = agent.getDataPoints();
							
							//Motion tween between two datapoints (-> time steps)
							int tweenCount = 0;
		
							//pick preattentive agent color
							int[] agentColor = colors.get(currentAgent);
							
							//first check if there is any datapoint stored to the current time step
							if (dataPoints.get(timeSteps.get(iInt)) != null) 
							{
								//get the datapoint
								DataPoint currentDataPoint = (DataPoint) dataPoints.get((timeSteps.get(iInt))); 
								
								float posX;
								float posY;
								
								//calculate tweened x/y position
								if ((tweenCount<dataPoints.size()-2))
								{
									DataPoint nextDataPoint = (DataPoint) dataPoints.get((timeSteps.get(iInt+1)));
									
									if (nextDataPoint==null)
										nextDataPoint = currentDataPoint;
										
									posX = (float)(        (  ((currentDataPoint.getPosX()-minPosX)*(1-(i-Math.floor(i))))
														   +  ((nextDataPoint.getPosX()-minPosX)*(i-Math.floor(i)))        )
														   / factorX);
									
									posY = (float)(( (  	(currentDataPoint.getPosY()-minPosY)*(1-(i-Math.floor(i))))
														   +  ((nextDataPoint.getPosY()-minPosY)*(i-Math.floor(i)))        )
														   / factorY);
										
										
								}
								else
								{
									posX = (float)((currentDataPoint.getPosX()-minPosX) / factorX);
									posY = (float)((currentDataPoint.getPosY()-minPosY) / factorY);
								}
								
								//draw trajectories
								for (int j = 1; j <= iInt; j++)
								{
									
									if ( ((dataPoints.get(timeSteps.get(j)) != null)) && ((dataPoints.get((timeSteps.get(j-1))) != null)) )
									{
										DataPoint trajectoryDataPoint1 = dataPoints.get(timeSteps.get(j));
										DataPoint trajectoryDataPoint2 = dataPoints.get(timeSteps.get(j-1));
										
										smooth();
										strokeWeight(4); 
										
										//make far away trajectories more transparent
										float jFloat = (float)j;
										float iFloat = (float)iInt;
										stroke(agentColor[0], agentColor[1],agentColor[2],255-(int)(255f*((iFloat+1f-jFloat)/iFloat)));
										
										line((float)((trajectoryDataPoint1.getPosX()-minPosX)/factorX),
											 (float)((trajectoryDataPoint1.getPosY()-minPosY)/factorY),
											 (float)((trajectoryDataPoint2.getPosX()-minPosX)/factorX),
											 (float)((trajectoryDataPoint2.getPosY()-minPosY)/factorY));
										
									}
									
								}
								
								//line from last position to tweened agent position
								line((float) ((currentDataPoint.getPosX() - minPosX) / factorX),
									 (float) ((currentDataPoint.getPosY() - minPosY) / factorY),
									 posX, posY);
								
								noStroke();
									
								//increment tween
								tweenCount++;
								
								//draw agent
								fill(color(agentColor[0], agentColor[1],agentColor[2],255));
								ellipse (posX, posY, agentSize, agentSize);
							
							}
							currentAgent++;
							
						}
			
					}
					
					
					
				}
			}
			
		}
		
	}

	public void setPositionRange(Point max, Point min)
	{
		// TODO Auto-generated method stub

	}

	public void setAnimationSpeed(Float speed)
	{
		// TODO Auto-generated method stub
		c = 100;
		// c = speed;

	}

	public void playScenario()
	{
		if (!rewind)
			i = 0;
		else
			i = timeSteps.size()-1;
		
	}
	
	public void togglePause()
	{
		paused = !paused;
		
	}

	public void rewindScenario()
	{
		rewind = !rewind;
		
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
		setAgentColors(agents.size());
		this.agents = agents;
		this.timeSteps = timeSteps;
		this.currentTime = timeSteps.getLast();
		console.println("update view at " + this.currentTime + "!");
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
