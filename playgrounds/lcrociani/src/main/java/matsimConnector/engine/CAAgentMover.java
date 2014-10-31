package matsimConnector.engine;

import matsimConnector.agents.Pedestrian;
import matsimConnector.environment.TransitionArea;
import matsimConnector.utility.LinkUtility;
import pedCA.context.Context;
import pedCA.engine.AgentMover;
import pedCA.output.Log;

public class CAAgentMover extends AgentMover {

	public CAAgentMover(Context context) {
		super(context);
	}

	public void step(){
		for(int index=0; index<getPopulation().size(); index++){
			Pedestrian pedestrian = (Pedestrian)getPopulation().getPedestrian(index);
			if(pedestrian.isEnteringEnvironment()){
				Log.log(pedestrian.toString() + " Moving inside Pedestrian Grid");
				moveToCA(pedestrian);
			}else if (pedestrian.isFinalDestinationReached()){
				Log.log(pedestrian.toString()+" Moving to CAQLink.");
				moveToQ(pedestrian);
			}
			else if (pedestrian.isArrived()){
				Log.log(pedestrian.toString()+" Exited.");
				delete(pedestrian);
				index--;
			}
			else{
				pedestrian.move();
			}
		}
	}
	
	private void delete(Pedestrian pedestrian) {
		pedestrian.moveToUniverse();
		getPopulation().remove(pedestrian);
	}

	private void moveToCA(Pedestrian pedestrian) {
		// TODO change link of the vehicle
		
		
		pedestrian.moveToEnvironment();
	}

	private void moveToQ(Pedestrian pedestrian) {
		// TODO add vehicle to CAQLink right here
		//
		
		TransitionArea transitionArea = LinkUtility.getDestinationTransitionArea(pedestrian.getVehicle());
		pedestrian.moveToTransitionArea(transitionArea);
	}
	
}
