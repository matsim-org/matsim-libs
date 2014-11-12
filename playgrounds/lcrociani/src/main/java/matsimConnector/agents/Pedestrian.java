package matsimConnector.agents;

import matsimConnector.environment.TransitionArea;
import matsimConnector.utility.Constants;
import matsimConnector.utility.IdUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import pedCA.agents.Agent;
import pedCA.context.Context;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.Neighbourhood;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.TacticalDestination;
import pedCA.output.Log;

public class Pedestrian extends Agent {

	private Id<Pedestrian> Id;
	private QVehicle vehicle; 
	private TransitionArea transitionArea;
	private boolean finalDestinationReached;
	private TacticalDestination originMarker;
	private Neighbourhood nextStepNeighbourhood;
	
	public Double lastTimeCheckAtExit = null;		
	
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
		if (finalDestinationReached && transitionArea!=null && getStaticFFValue(getPosition())==0.){
			exit();
		}
	}
	
	private void perceptIfFinalDestination(double now) {
		if (getStaticFFValue(getPosition())==0. && transitionArea == null){
			finalDestinationReached = true;
			if (now < Constants.CA_TEST_END_TIME){
				calculateNextStepNeighbourhood();
			}
		}else if(transitionArea == null){
			finalDestinationReached = false;
		}
	}

	public void move(double now){
		if (transitionArea!=null){
			if(!getPosition().equals(getNewPosition())){
				transitionArea.moveTo(this, getNewPosition());
				Log.log("MOVED FROM "+getPosition().toString()+" TO "+getNewPosition().toString()+" INSIDE TRANSITION AREA");
				setPosition(getNewPosition());
			}
		}else {
			if (finalDestinationReached && !getNewPosition().equals(getPosition()))
				lastTimeCheckAtExit = now;
			super.move();
			perceptIfFinalDestination(now);
		}
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
		return transitionArea != null && getStaticFFValue(getPosition())==0. && !finalDestinationReached;
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

	//TODO TEST!!!
	private void calculateNextStepNeighbourhood() {
		nextStepNeighbourhood = new Neighbourhood(getPedestrianGrid().getFreePositions(originMarker.getCells()));
		if (nextStepNeighbourhood.size() == 0)
			nextStepNeighbourhood.add(getPosition());
	}	
	
	public void moveToEnvironment(){
		GridPoint nextPosition = transitionArea.convertTAPosToEnvPos(getPosition());
		this.originMarker = transitionArea.getReferenceDestination();
		leaveTransitionArea();
		enterPedestrianGrid(nextPosition);
	}
	
	public void moveToTransitionArea(TransitionArea transitionArea) {
		setTransitionArea(transitionArea);
		GridPoint translatedPosition = transitionArea.convertEnvPosToTAPos(getPosition());
		leavePedestrianGrid();
		enterTransitionArea(translatedPosition);
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
	
	@Override
	public Neighbourhood getNeighbourhood(){
		if (nextStepNeighbourhood != null && transitionArea == null){
			Neighbourhood result = nextStepNeighbourhood;
			nextStepNeighbourhood = null;
			return result;
		}
		if (transitionArea != null){
			nextStepNeighbourhood = null;
			return transitionArea.getNeighbourhood(getPosition());
		}else
			return super.getNeighbourhood();
	}
	
	@Override
	public boolean checkOccupancy(GridPoint neighbour) {
		if(isAtEnvironmentBorder(neighbour)){
			if (transitionArea != null)
				return transitionArea.isOccupied(neighbour) || getPedestrianGrid().isOccupied(transitionArea.convertTAPosToEnvPos(neighbour));
			else{
				TransitionArea neighbourTA = getDestination(neighbour).getTransitionArea();
				return neighbourTA.isOccupied(neighbourTA.convertEnvPosToTAPos(neighbour)) || getPedestrianGrid().isOccupied(neighbour);
			}
		}		
		if (transitionArea != null)
			return transitionArea.isOccupied(neighbour);
		return getPedestrianGrid().isOccupied(neighbour);
	}
	
	private TacticalDestination getDestination(GridPoint neighbour) {
		TacticalDestination result = null;
		for (Destination dest : getContext().getMarkerConfiguration().getDestinations())
			if (dest instanceof TacticalDestination && dest.getCells().contains(neighbour)){
				result = (TacticalDestination)dest;
				break;
			}
		return result;
	}

	//TODO TEST!!!
	public boolean isAtEnvironmentBorder(GridPoint position) {
		if (transitionArea != null){
			return transitionArea.isAtBorder(position);
		} else
			return getDestination(position)!=null;
	}

	/**
	 * These two methods return the "real" position of the agent in the environment,
	 * considering transitionAreas as extension of the grid at its borders
	 * */
	public GridPoint getRealPosition(){
		if (transitionArea != null){
			GridPoint pos = new GridPoint(getPosition().getX(),getPosition().getY());
			return transitionArea.convertTAPosToEnvPos(pos);
		}
		return getPosition();
	}
	
	public GridPoint getRealNewPosition(){
		if (transitionArea != null){
			GridPoint pos = new GridPoint(getNewPosition().getX(),getNewPosition().getY());
			return transitionArea.convertTAPosToEnvPos(pos);
		}
		return getNewPosition();
	}
	
	public TacticalDestination getOriginMarker(){
		return originMarker;
	}
}