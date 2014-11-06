package pedCA.engine;

import java.util.ArrayList;

import matsimConnector.agents.Pedestrian;
import matsimConnector.environment.TransitionArea;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import pedCA.agents.Agent;
import pedCA.agents.Population;
import pedCA.context.Context;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.Start;
import pedCA.output.Log;
import pedCA.utility.Lottery;

public class AgentsGenerator {
	private Context context;
	
	public AgentsGenerator(Context context){
		this.context = context;
	}
	
	public void step(){
		for(Start start : getStarts()){
			generateFromStart(start);
		}
	}

	private void generateFromStart(Start start){
		int howMany = start.toBeGenerated();
		ArrayList<GridPoint> usedCells = getPedestrianGrid().freePositions(start.getCells());
		if (howMany>usedCells.size()){
			Log.warning("not enough space in start "+start.toString());
		}
		else{
			usedCells = Lottery.extractObjects(usedCells,howMany);
		}
		for(GridPoint p : usedCells){
			generateSinglePedestrian(p);
			start.notifyGeneration();
		}
	}
	
	private void generateSinglePedestrian(GridPoint initialPosition) {
		int pedID = getPopulation().getPedestrians().size();
		Destination destination = getRandomDestination();
		Agent pedestrian = new Agent(pedID,initialPosition,destination,context);
		getPopulation().addPedestrian(pedestrian);
		context.getPedestrianGrid().addPedestrian(initialPosition, pedestrian);
	}

	//FOR MATSIM CONNECTOR
	public Pedestrian generatePedestrian(GridPoint initialPosition, int destinationId, QVehicle vehicle, TransitionArea transitionArea){
		int pedID = getPopulation().getPedestrians().size();
		Destination destination = context.getMarkerConfiguration().getDestination(destinationId);
		Agent agent = new Agent(pedID,initialPosition,destination,context);
		Pedestrian pedestrian = new Pedestrian(agent, vehicle, transitionArea);
		getPopulation().addPedestrian(pedestrian);
		//context.getPedestrianGrid().addPedestrian(initialPosition, pedestrian);
		return pedestrian;
	}
	
	//FOR MATSIM CONNECTOR
	public Context getContext(){
		return context;
	}
	
	//FOR MATSIM CONNECTOR
	public GridPoint getFreePosition(int destinationId){
		ArrayList<GridPoint> cells = getContext().getMarkerConfiguration().getDestination(destinationId).getCells();
 		ArrayList<GridPoint> usedCells = getPedestrianGrid().freePositions(cells); 
 		return Lottery.extractObjects(usedCells,1).get(0);
	}
	
	private Destination getRandomDestination() {
		return Lottery.extractObject(context.getMarkerConfiguration().getDestinations());
	}
	
	private Population getPopulation(){
		return context.getPopulation();
	}

	private ArrayList<Start> getStarts(){
		return context.getMarkerConfiguration().getStarts();
	}
	
	private PedestrianGrid getPedestrianGrid(){
		return context.getPedestrianGrid();
	}
}
