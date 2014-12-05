package matsimConnector.agents;

import matsimConnector.environment.TransitionArea;
import matsimConnector.utility.Constants;
import matsimConnector.utility.IdUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import pedCA.agents.Agent;
import pedCA.context.Context;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.grid.neighbourhood.Neighbourhood;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.FinalDestination;
import pedCA.utility.NeighbourhoodUtility;

public class Pedestrian extends Agent {

	private Id<Pedestrian> Id;
	private QVehicle vehicle; 
	private TransitionArea transitionArea;
	private boolean finalDestinationReached;
	private FinalDestination originMarker;
	private Neighbourhood nextStepNeighbourhood;
	
	public Double lastTimeCheckAtExit = null;		
	
	public Pedestrian(Agent agent, QVehicle vehicle, TransitionArea transitionArea){
		this(agent.getID(),agent.getPosition(), agent.getDestination(), agent.getContext());
		this.vehicle = vehicle;
		finalDestinationReached = false;
		this.transitionArea = transitionArea;
		this.originMarker = transitionArea.getReferenceDestination();
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
			}
			updateHeading();
			setPosition(getNewPosition());
		}else {
			if (!isWaitingToSwap() && finalDestinationReached && !getNewPosition().equals(getPosition()))
				lastTimeCheckAtExit = now;
			super.move();
			perceptIfFinalDestination(now);
		}
	}

	@Override
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
	
	@Override
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
		Neighbourhood neighbourhood = NeighbourhoodUtility.calculateMooreNeighbourhood(position);//getPedestrianGrid().getFreePositions(originMarker.getCells()));
		nextStepNeighbourhood = new Neighbourhood();
		for (GridPoint neighbour : neighbourhood.getObjects()){
			if (neighbour.getX()>0)
				neighbour.setX(neighbour.getX()%getPedestrianGrid().getColumns());
			else
				neighbour.setX(neighbour.getX()+getPedestrianGrid().getColumns());
			if (originMarker.getCells().contains(neighbour))
				nextStepNeighbourhood.add(neighbour);
		}
		if (nextStepNeighbourhood.size() == 0)
			nextStepNeighbourhood.add(getPosition());
	}	
	
	public void refreshDestination() {
		Id<Link> linkId = vehicle.getDriver().getCurrentLinkId();
		int destinationId = IdUtility.linkIdToDestinationId(linkId);
		this.destination = context.getMarkerConfiguration().getDestination(destinationId);
		this.finalDestinationReached = false;
	}
	
	public void moveToEnvironment(){
		GridPoint nextPosition = transitionArea.convertTAPosToEnvPos(getPosition());
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
	
	public PedestrianGrid getUsedPedestrianGrid(){
		if (transitionArea != null)
			return transitionArea;
		return getPedestrianGrid();
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
	
	protected boolean canSwap(GridPoint neighbour, PedestrianGrid pedestrianGrid) {
		if (finalDestinationReached && pedestrianGrid.containsPedestrian(neighbour) && transitionArea == null){
		 	return ((Pedestrian)pedestrianGrid.getPedestrian(neighbour)).finalDestinationReached;
		}			
		return super.canSwap(neighbour, pedestrianGrid);
	}
	/*
	protected boolean isInFrontCell(GridPoint neighbour) {
		if (nextStepNeighbourhood != null)
			return false;
		return super.isInFrontCell(neighbour);
	}*/
	
	@Override
	protected boolean checkOccupancy(GridPoint neighbour) {
		//TODO FIX THE PROBLEM WITH TRANSITION AREAS
		if(isAtEnvironmentBorder(neighbour)){
			if (transitionArea != null)
				return checkOccupancy(neighbour,transitionArea) || getPedestrianGrid().isOccupied(transitionArea.convertTAPosToEnvPos(neighbour));						//transitionArea.isOccupied(neighbour) || getPedestrianGrid().isOccupied(transitionArea.convertTAPosToEnvPos(neighbour));//checkOccupancy(neighbour,transitionArea) || checkOccupancy(transitionArea.convertTAPosToEnvPos(neighbour),getPedestrianGrid());
			else{
				TransitionArea neighbourTA = getFinalDestination(neighbour).getTransitionArea();
				return neighbourTA.isOccupied(neighbourTA.convertEnvPosToTAPos(neighbour)) || checkOccupancy(neighbour,getPedestrianGrid());//checkOccupancy(neighbourTA.convertEnvPosToTAPos(neighbour),neighbourTA) || checkOccupancy(neighbour,getPedestrianGrid());
			}
		}		
		return checkOccupancy(neighbour, getUsedPedestrianGrid());
	}
	
	private FinalDestination getFinalDestination(GridPoint neighbour) {
		FinalDestination result = null;
		for (Destination dest : getContext().getMarkerConfiguration().getDestinations())
			if (dest instanceof FinalDestination && dest.getCells().contains(neighbour)){
				result = (FinalDestination)dest;
				break;
			}
		return result;
	}

	//TODO TEST!!!
	public boolean isAtEnvironmentBorder(GridPoint position) {
		if (transitionArea != null){
			return transitionArea.isAtBorder(position);
		} else
			return getFinalDestination(position)!=null;
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
	
	public FinalDestination getOriginMarker(){
		return originMarker;
	}
}