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
	private HashMap<Integer, DataPoint> nodes;
	private HashMap<Integer, int[]> links;
	//private LinkedList<HashMap<Integer, Agent>>;

	private final int width;
	private final int height;

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

	private boolean mousePressed = false;

	private final Console console;

	private int traceTimeRange = 3;

	private LinkedList<Scene> scenes;

	private ArrayList<int[]> colors;

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

	public P3DRenderer(boolean liveMode, int traceTimeRange, Console console, int width, int height)
	{
		this.traceTimeRange = traceTimeRange;
		this.liveMode = liveMode;
		this.console = console;

		this.width = width;
		this.height = height;


		setAgentColors(255);

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
		this.colors = new ArrayList<int[]>();
		for (int j = 0; j < agentCount; j++)
		{
			//determine color
			int[] color = {(int)((((float)agentCount-(float)j)/agentCount)*255),
					(int)((double)(((float)agentCount-(float)j)/agentCount)*255),
					(int)(255-(double)(((float)agentCount-(float)j)/agentCount)*255)};

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
	private long oldTime;

	@Override
	public void setup()
	{
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

		background(33);
		fill(0, 0, 0);

		//FIRST

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
				line((float)((fromDataPoint.getPosX()-this.minPosX)/this.factorX),
						(float)((fromDataPoint.getPosY()-this.minPosY)/this.factorY),
						(float)((ToDataPoint.getPosX()-this.minPosX)/this.factorX),
						(float)((ToDataPoint.getPosY()-this.minPosY)/this.factorY));


			}
			noStroke();
		}


		//SECOND

		//draw agents
		if (this.liveMode)
		{
			if ((this.extremeValues != null) && (this.factorX == 0.0f))
			{
				//get max values
				float maxWidth = (float)(this.extremeValues[0]-this.extremeValues[3]);
				float maxHeight = (float)(this.extremeValues[1]-this.extremeValues[4]);

				//refactor min/max values of event+network file
				this.factorX = maxWidth / this.width;
				this.factorY = maxHeight / this.height;

				//console.println(((this.maxPosX-this.minPosX) /factorX));
			}



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

			int sceneCount = 0;
			boolean drawAgent = false;

			//console.println(timeSteps);

			if ((this.agents != null) && (this.timeSteps != null))
			{
				if (this.timeSteps.size()>0)
				{

					//console.println("----- + + + + ----- " + timeSteps.size() + "| agents:" + agents.toString());

					//Iterate through all agents and display the current data point + traces
					Iterator agentsIterator = this.agents.entrySet().iterator();
					int agentCount = 0;

					//While there are still agents in the agents array
					while (agentsIterator.hasNext())
					{

						//Get current agent
						Map.Entry pairs = (Map.Entry) agentsIterator.next();
						Agent currentAgent = (Agent)pairs.getValue();
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

							//draw node trajectories if there is more then one datapoint for the current agent
							if (dataPoints.size() > 1)
							{
								//number of lines (trajectories) to draw (between 2 and traceTimeRange)
								int traceDisplayCount = Math.min(this.traceTimeRange, dataPoints.size());

								//loop through the datapoints with the corresponding timesteps
								for (int timeStep = 0; timeStep < traceDisplayCount-1; timeStep++)
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
										line((float)((currentDataPoint.getPosX() - this.minPosX) / this.factorX),
												(float)((currentDataPoint.getPosY() - this.minPosY) / this.factorY),
												(float)((nextDataPoint.getPosX()    - this.minPosX) / this.factorX),
												(float)((nextDataPoint.getPosY()    - this.minPosY) / this.factorY));
									}

								}




							}

							DataPoint lastDataPoint = dataPoints.get(this.currentTime);
							//console.println("current agent: " + currentAgent.get);
							//							console.println("@________@________@: " + timeSteps.toString());
							//							console.println("TS GET LAST " + timeSteps.getLast());
							//							console.println("AVAILABLE DP: " + dataPoints.toString());
							//							console.println("| " + lastDataPoint.toString());


							if (lastDataPoint != null)
							{
								//draw agent

								float posX = (float)((lastDataPoint.getPosX()-this.minPosX) / this.factorX);
								float posY = (float)((lastDataPoint.getPosY()-this.minPosY) / this.factorY);
								this.console.println("********* MINPOS X: "+ this.minPosX +" | FACT X: " + this.factorX + "***************");
								this.console.println("********* MINPOS Y: "+ this.minPosY +" | FACT Y: " + this.factorY + "***************");
								this.console.println("********* DRAW: posX:" + posX + "| posY: " + posY + "***************");

								//stroke
								//								SMOOTH();
								//								STROKEWEIGHT(4);
								//								STROKE(0,0,0);

								//noStroke();

								int halfAgentSize = (int) this.agentSize / 2;

								if    ((this.mouseX < posX+halfAgentSize) && (this.mouseX > posX-halfAgentSize)
										&& (this.mouseY < posY+halfAgentSize) && (this.mouseY > posY-halfAgentSize))
									fill(255, 255, 255,255);
								else
									fill(color(agentColor[0], agentColor[1],agentColor[2],255));

								ellipse (posX, posY, this.agentSize, this.agentSize);

								fill(0, 0, 0, 255);
								text(currentAgentID,posX+(this.agentSize/2)-12,posY+(this.agentSize/2)-6);

							}
							else
								this.console.println("agent " + currentAgentID + " missing at " + this.currentTime + " (" + this.timeSteps.getLast() + ") :" + dataPoints.toString());




						}
						agentCount++;
					}

				}
			}


		}
		else
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
								DataPoint currentDataPoint = dataPoints.get((this.timeSteps.get(this.iInt)));

								float posX;
								float posY;

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
