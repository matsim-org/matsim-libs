package pedCA.agents;

import java.util.ArrayList;

import matsimConnector.utility.MathUtility;
import pedCA.context.Context;
import pedCA.environment.grid.FloorFieldsGrid;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.grid.WeightedCell;
import pedCA.environment.grid.neighbourhood.Neighbourhood;
import pedCA.environment.markers.Destination;
import pedCA.utility.Constants;
import pedCA.utility.DirectionUtility;
import pedCA.utility.DirectionUtility.Heading;
import pedCA.utility.Lottery;

public class Agent extends PhysicalObject{
	
	private final int Id;
	protected final Context context;
	private GridPoint nextpos; 
	private Heading heading;
	protected Destination destination;
	private boolean arrived;
	
	private boolean wantToSwap;
	private int stepToPerformSwap;
	private boolean hasToSwap;
	
	public Agent(int Id, GridPoint position, Destination destination, Context context){
		this.Id = Id;
		this.position = nextpos = position;	
		this.context = context;
		this.destination = destination;
		arrived = false;
		//TODO ENSURE THAT X IS NEVER ASSUMED DURING THE SIMULATION
		heading = Heading.X;
		wantToSwap = false;
		stepToPerformSwap = 0;
		hasToSwap = false;
	}
	
	public void updateChoice(){
		if (isWaitingToSwap()){
			stepToPerformSwap--;
			hasToSwap = stepToPerformSwap==0;
		}
		else{
			percept();
			if (!isArrived()){
				ArrayList<WeightedCell> probabilityValues = evaluate();
				choose(probabilityValues);
				checkBidirectionalSwitch();
			}
		}
	}
	
	private void checkBidirectionalSwitch() {
		if(!nextpos.equals(position) && getUsedPedestrianGrid().isOccupied(nextpos))
			wantToSwap = true;
	}
	
	public PedestrianGrid getUsedPedestrianGrid() {
		return getPedestrianGrid();
	}

	public int calculateStepToPerformSwap(){
		double pedestrianDensity = calculatePerceivedDensity();
		double delay_seconds = Math.pow(pedestrianDensity*.61,1.45)*0.4;//Math.pow(pedestrianDensity*.61,1.43)*0.39;
		double delay = delay_seconds/Constants.STEP_DURATION;
		int result = (int)delay;//Constants.STEP_FOR_BIDIRECTIONAL_SWAPPING;
		double probability = delay-result;
		if (Lottery.simpleExtraction(probability))
			result++;
		return result;
	}

	public double calculatePerceivedDensity() {
		return getUsedPedestrianGrid().getPedestrianDensity(position);		
		/* THIS IS FOR THE SHIFT OF THE DENSITY PERCEPTION
		GridPoint shiftedPosition = getShiftedPosition();
		double pedestrianDensity = getUsedPedestrianGrid().getPedestrianDensity(shiftedPosition);
		return pedestrianDensity;*/
	}
	
	public GridPoint getShiftedPosition(){
		GridPoint direction = DirectionUtility.convertHeadingToGridPoint(this.heading);
		System.out.println(this.Id + "pos: " + position.toString());
		GridPoint shiftedPosition = new GridPoint(position.getX() + direction.getX(),position.getY() + direction.getY());
		System.out.println(this.Id + "shifted: " + shiftedPosition.toString());
		if (this.getUsedPedestrianGrid().isWalkable(shiftedPosition))
			return shiftedPosition;
		else
			return position;
	}
	
	public void startBidirectionalSwitch(int stepToPerformSwap){
		wantToSwap = false;
		this.stepToPerformSwap = stepToPerformSwap;
	}

	public int getStepToPerformSwap() {
		return stepToPerformSwap;
	}

	protected void percept(){
		if (getStaticFFValue(position)==0.)
			exit();
	}
	
	protected ArrayList<WeightedCell> evaluate(){
		double myPositionValue = getStaticFFValue(position);
		double neighbourValue;
		double occupation = 0.0;
		double probabilitySum = 0.0;
		
		Neighbourhood neighbourhood = getNeighbourhood();
		ArrayList<WeightedCell> probabilityValues = new ArrayList<WeightedCell>();

		for(int index= 0; index<neighbourhood.size();index++){
			GridPoint neighbour = neighbourhood.get(index);
			neighbourValue = getStaticFFValue(neighbour);
			
			occupation = 0.0;
			if((!neighbour.equals(position)) && checkOccupancy(neighbour))
				occupation = 1.0;
			
			double p = utilityFunction(myPositionValue, neighbourValue,	occupation);
			
			probabilitySum += p;
			probabilityValues.add(new WeightedCell(neighbour,p));
		}
		Lottery.normalizeProbabilities(probabilityValues, probabilitySum);
		return probabilityValues;
	}

	private double utilityFunction(double myPositionValue, double neighbourValue, double occupation) {
		double utilityValue = Math.pow(Math.E, Constants.KS*(myPositionValue - neighbourValue));
		utilityValue = utilityValue*(1-(Constants.PHI*occupation));				//FORMULA => Math.pow(Math.E, Constants.KS*(MyPositionValue - neighbourValue))*epsilon*(1-(Constants.PHI*n));	
		return utilityValue;
	}
	
	protected void choose(ArrayList<WeightedCell> probabilityValues){
		WeightedCell winningCell = 	Lottery.pickWinner(probabilityValues);
		if(winningCell == null)
			nextpos = position;
		else
			nextpos = new GridPoint(winningCell.x, winningCell.y);
	}
	
	public void move(){
		if(!isWaitingToSwap()&&!hasToSwap){
			if(!position.equals(nextpos)){
				getPedestrianGrid().moveTo(this, nextpos);
			}
			updateHeading();
			setPosition(nextpos);
		}else if(!isWaitingToSwap()){
			getUsedPedestrianGrid().moveToWithoutShadow(this, nextpos);
			setPosition(nextpos);
			hasToSwap = false;
		}
	}

	protected boolean checkOccupancy(GridPoint neighbour) {
		return checkOccupancy(neighbour, getPedestrianGrid());
	}
	
	protected boolean checkOccupancy(GridPoint neighbour, PedestrianGrid pedestrianGrid){
		if (pedestrianGrid.isOccupied(neighbour)) {
			return !canSwap(neighbour, pedestrianGrid);
		}
		return false;
	}

	protected boolean canSwap(GridPoint neighbour, PedestrianGrid pedestrianGrid) {
		if (pedestrianGrid.containsPedestrian(neighbour)){
			Agent neighbourAgent = pedestrianGrid.getPedestrian(neighbour);
			int deltaFF = (int)Math.round(neighbourAgent.getStaticFFValue(neighbour) - neighbourAgent.getStaticFFValue(position));
			return deltaFF >0;
		}
		return false;
		
		/**
		if (pedestrianGrid.containsPedestrian(neighbour) && isInFrontCell(neighbour)){ 
			Heading neighbourHeading = pedestrianGrid.getPedestrian(neighbour).getHeading();
			return counterflowHeading(neighbourHeading);
		}
		return false;*/
	}
	/*
	protected boolean isInFrontCell(GridPoint neighbour) {
		//TODO MANAGE nextStepNeighbourhood
		GridPoint deltaPosition = MathUtility.gridPointDifference(neighbour, position);
		return DirectionUtility.convertGridPointToHeading(deltaPosition).equals(heading);
	}

	private boolean counterflowHeading(Heading neighbourHeading) {
		if (getHeading().equals(Heading.X))
			return false;
		GridPoint directionSum = MathUtility.gridPointSum(DirectionUtility.convertHeadingToGridPoint(getHeading()), DirectionUtility.convertHeadingToGridPoint(neighbourHeading));
		return directionSum.getX() == 0 && directionSum.getY() == 0;
	}
	*/
	protected void updateHeading() {
		if(!position.equals(nextpos)){
			GridPoint positionDelta = MathUtility.gridPointDifference(nextpos, position);
			heading = DirectionUtility.convertGridPointToHeading(positionDelta);
		}else{
			//heading = Lottery.extractHeading();
		}
	}

	protected void setPosition(GridPoint position) {
		this.position = position;
	}
	
	protected void setNewPosition(GridPoint position){
		this.nextpos = position;
	}
	
	public void exit(){
		arrived = true;
	}
	
	public void enterPedestrianGrid(GridPoint position){
		getPedestrianGrid().addPedestrian(position, this);
		setPosition(position);
	}
	
	public void leavePedestrianGrid() {
		getPedestrianGrid().removePedestrian(getPosition(), this);
		setPosition(null);
	}
	
	public boolean isArrived(){
		return arrived;
	}
	
	public void revertChoice() {
		nextpos = position;
	}
	
	public void revertWillingToSwap(){
		revertChoice();
		wantToSwap = false;
	}

	protected Double getStaticFFValue(GridPoint gridPoint) {
		return getStaticFF().getCellValue(destination.getLevel(), gridPoint);
	}

	private FloorFieldsGrid getStaticFF(){
		return context.getFloorFieldsGrid();
	}
	
	protected PedestrianGrid getPedestrianGrid(){
		return context.getPedestrianGrid();
	}
	
	public Neighbourhood getNeighbourhood(){
		return getStaticFF().getNeighbourhood(position);
	}
	
	public int getID(){
		return Id;
	}
	
	public Destination getDestination(){
		return destination;
	}
	
	public Context getContext(){
		return context;
	}
	
	public GridPoint getNewPosition(){
		return nextpos;
	}
	
	public Heading getHeading(){
		return heading;
	}
	
	public boolean isWillingToSwap() {
		return wantToSwap;
	}
	
	public boolean isWaitingToSwap() {
		return stepToPerformSwap>0;
	}

	public String toString(){
		return "Pedestrian "+getID();
	}
}