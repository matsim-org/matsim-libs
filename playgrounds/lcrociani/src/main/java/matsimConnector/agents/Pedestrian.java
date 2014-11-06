package matsimConnector.agents;

import matsimConnector.environment.TransitionArea;
import matsimConnector.utility.IdUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import pedCA.agents.Agent;
import pedCA.context.Context;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.markers.Destination;
import pedCA.output.Log;

public class Pedestrian extends Agent {

	private Id<Pedestrian> Id;
	private QVehicle vehicle; 
	private TransitionArea transitionArea;
	private boolean finalDestinationReached;
	
	public Pedestrian(Agent agent, QVehicle vehicle, TransitionArea transitionArea){
		this(agent.getID(),agent.getPosition(), agent.getDestination(), agent.getContext());
		this.vehicle = vehicle;
		finalDestinationReached = false;
		this.transitionArea = transitionArea;
		enterTransitionArea(getPosition());
	}
	
	private Pedestrian(int Id, GridPoint position, Destination destination, Context context) {
		super(Id, position, destination, context);
		generateId();
	}
	
	@Override
	public void percept(){
		if (finalDestinationReached && getStaticFFValue(getPosition())==0.){
			exit();
		}
		else if (transitionArea == null && getStaticFFValue(getPosition())==0.)
			finalDestinationReached = true;
	}
	
	@Override
	public void move(){
		if (transitionArea!=null){
			if(!getPosition().equals(getNewPosition())){
				transitionArea.moveTo(this, getNewPosition());
				Log.log("MOVED FROM "+getPosition().toString()+" TO "+getNewPosition().toString()+" INSIDE TRANSITION AREA");
				setPosition(getNewPosition());
			}
		} else 
			super.move();
	}
	
	protected Double getStaticFFValue(GridPoint gridPoint) {
		if (transitionArea!=null)
			return getTransitionAreaFieldValue(gridPoint);
		else
			return super.getStaticFFValue(gridPoint);
	}

	public double getTransitionAreaFieldValue(GridPoint gridPoint){
		return transitionArea.getSFFValue(gridPoint,finalDestinationReached);
	}
	
	public void setTransitionArea(TransitionArea transitionArea){
		this.transitionArea = transitionArea;
	}
	
	private void generateId(){
		Id = IdUtility.createPedestrianId(super.getID());
	}

	public Id<Pedestrian> getId(){
		return Id;
	}
	
	public QVehicle getVehicle(){
		return vehicle;
	}
	
	protected void setPosition(GridPoint position){
		super.setPosition(position);
	}
	
	public boolean isEnteringEnvironment(){
		return transitionArea != null && getStaticFFValue(getPosition())==0 && !finalDestinationReached;
	}
	
	public boolean isFinalDestinationReached(){
		return finalDestinationReached;
	}
	
	public boolean hasLeftEnvironment(){
		return finalDestinationReached && transitionArea != null;
	}
	
	public void moveToUniverse(){
		if (transitionArea != null)
			leaveTransitionArea();
		else
			leavePedestrianGrid();
	}
	
	public void moveToEnvironment(){
		GridPoint myPosition = new GridPoint(getPosition().getX(),getPosition().getY());
		GridPoint nextPosition = transitionArea.convertTAPosToEnvPos(myPosition);
		leaveTransitionArea();
		enterPedestrianGrid(nextPosition);
	}
	
	public void moveToTransitionArea(TransitionArea transitionArea) {
		setTransitionArea(transitionArea);
		GridPoint nextPosition = transitionArea.convertEnvPosToTAPos(getPosition());
		leavePedestrianGrid();
		enterTransitionArea(nextPosition);
	}

	private void enterTransitionArea(GridPoint position) {
		transitionArea.addPedestrian(position, this);
		setPosition(position);
	}
	
	private void leaveTransitionArea(){
		transitionArea.removePedestrian(getPosition(), this);
		transitionArea = null;
		setPosition(null);
	}
}