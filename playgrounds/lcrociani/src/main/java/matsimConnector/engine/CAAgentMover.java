package matsimConnector.engine;

import matsimConnector.agents.Pedestrian;
import matsimConnector.environment.TransitionArea;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.CAQLink;

import pedCA.context.Context;
import pedCA.engine.AgentMover;
import pedCA.output.Log;

public class CAAgentMover extends AgentMover {

	private CAEngine engineCA;

	public CAAgentMover(CAEngine engineCA, Context context) {
		super(context);
		this.engineCA = engineCA;
	}

	public void step(double time){
		for(int index=0; index<getPopulation().size(); index++){
			Pedestrian pedestrian = (Pedestrian)getPopulation().getPedestrian(index);
			if(pedestrian.isEnteringEnvironment()){
				Log.log(pedestrian.toString() + " Moving inside Pedestrian Grid");
				moveToCA(pedestrian, time);
			}else if (pedestrian.isFinalDestinationReached() && !pedestrian.hasLeftEnvironment()){
				Log.log(pedestrian.toString()+" Moving to CAQLink.");
				moveToQ(pedestrian, time);
			}
			else if (pedestrian.isArrived()){
				Log.log(pedestrian.toString()+" Exited.");
				delete(pedestrian);
				index--;
			}
			else
				pedestrian.move();
		}
	}
	
	private void delete(Pedestrian pedestrian) {
		pedestrian.moveToUniverse();
		getPopulation().remove(pedestrian);
	}

	private void moveToCA(Pedestrian pedestrian, double time) {
		//Id<Link> currentLinkId = pedestrian.getVehicle().getDriver().getCurrentLinkId();
		Id<Link> nextLinkId = pedestrian.getVehicle().getDriver().chooseNextLinkId();
		
		//TODO RAISE LINKLEAVEEVENT
		//engineCA.getQCALink(currentLinkId).popFirstVehicle();
		
		
		pedestrian.getVehicle().getDriver().notifyMoveOverNode(nextLinkId);
		//pedestrian.getVehicle().getDriver().chooseNextLinkId();
		//pedestrian.getVehicle().getDriver().endLegAndComputeNextState(time);
		
		pedestrian.moveToEnvironment();
	}

	private void moveToQ(Pedestrian pedestrian, double time) {
		//Id<Link> currentLinkId = pedestrian.getVehicle().getDriver().getCurrentLinkId();
		Id<Link> nextLinkId = pedestrian.getVehicle().getDriver().chooseNextLinkId();
		
		//engineCA.getQCALink(currentLinkId).popFirstVehicle();
		
		pedestrian.getVehicle().getDriver().notifyMoveOverNode(nextLinkId);
		CAQLink lowResLink = engineCA.getCAQLink(nextLinkId);
		lowResLink.addFromUpstream(pedestrian.getVehicle());
		TransitionArea transitionArea = lowResLink.getTransitionArea();
		pedestrian.moveToTransitionArea(transitionArea);
	}
	
}
