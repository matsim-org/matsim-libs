package pedCA.agents;

import java.util.ArrayList;

import pedCA.context.Context;
import pedCA.environment.grid.FloorFieldsGrid;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.Neighbourhood;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.grid.WeightedCell;
import pedCA.environment.markers.Destination;
import pedCA.utility.Constants;
import pedCA.utility.Lottery;

enum Heading{
	N, NE, NO, S, SE, SO, E, O, X
}

public class Agent{
	
	private final int Id;
	private final Context context;
	private GridPoint position;
	private GridPoint nextpos; 
	private Double speed;
	private Heading heading;
	private Destination destination;
	private boolean arrived;
	
	public Agent(int Id, GridPoint position, Destination destination, Context context){
		this.Id = Id;
		this.position = nextpos = position;	
		this.context = context;
		this.destination = destination;
		arrived = false;
		//TODO NOT YET USED
		heading = Heading.X;
		speed = 1.6;
	}
	
	public void sequentialUpdate(){
		updateChoice();
		move();
	}
	
	public void updateChoice(){
		percept();
		if (!isArrived()){
			ArrayList<WeightedCell> probabilityValues = evaluate();
			choose(probabilityValues);
		}
	}
	
	public void percept(){
		if (getStaticFFValue(position)==0.)
			exit();
	}
	
	public ArrayList<WeightedCell> evaluate(){
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

	public boolean checkOccupancy(GridPoint neighbour) {
		return getPedestrianGrid().isOccupied(neighbour);
	}

	private double utilityFunction(double myPositionValue, double neighbourValue, double occupation) {
		double utilityValue = Math.pow(Math.E, Constants.KS*(myPositionValue - neighbourValue));
		utilityValue = utilityValue*(1-(Constants.PHI*occupation));				//FORMULA => Math.pow(Math.E, Constants.KS*(MyPositionValue - neighbourValue))*epsilon*(1-(Constants.PHI*n));	
		return utilityValue;
	}
	
	public void choose(ArrayList<WeightedCell> probabilityValues){
		WeightedCell winningCell = 	Lottery.pickWinner(probabilityValues);
		if(winningCell == null)
			nextpos = position;
		else
			nextpos = new GridPoint(winningCell.x, winningCell.y);
		//Log.log("Pedestrian "+getID()+" chose "+nextpos.toString());
	}
	
	public void move(){
		if(!position.equals(nextpos)){
			getPedestrianGrid().moveTo(this, nextpos);
			//Log.log("MOVED FROM "+position.toString()+" TO "+nextpos.toString());
			setPosition(nextpos);
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
	
	public GridPoint getPosition(){
		return position;
	}
	
	public GridPoint getNewPosition(){
		return nextpos;
	}
	
	public Heading getHeading(){
		return heading;
	}
	
	public double getSpeed(){
		return speed;
	}
	
	public String toString(){
		return "Pedestrian "+getID();
	}
}
