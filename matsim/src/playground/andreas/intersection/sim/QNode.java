package playground.andreas.intersection.sim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.trafficlights.data.SignalGroupSettings;
import org.matsim.trafficlights.data.SignalLane;

import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QNode extends QueueNode{

	private SignalSystemControlerImpl myNodeTrafficLightControler;
	
	public QNode(Node n, QueueNetwork queueNetwork) {
		super(n,  queueNetwork);
	}

	public void setSignalSystemControler(SignalSystemControlerImpl nodeControler){
		this.myNodeTrafficLightControler = nodeControler;
	}

	public SignalSystemControlerImpl getMyNodeTrafficLightControler() {
		return this.myNodeTrafficLightControler;
	}

	/** Simple moveNode, Complex one can be found in {@link QueueLink} */
	@Override
	public void moveNode(final double now) {

		if(this.myNodeTrafficLightControler != null){

			// Node is traffic light controlled

			SignalGroupSettings[] greenSignalGroups = this.myNodeTrafficLightControler.getGreenInLinks(now);

			if (greenSignalGroups.length != 0){

				for (int i = 0; i < greenSignalGroups.length; i++) {
					SignalGroupSettings signalGroupSetting = greenSignalGroups[i];

					Link link = this.getNode().getInLinks().get((signalGroupSetting.getSignalGroupDefinition().getLinkId()));
					
					QLink qLink = (QLink) this.queueNetwork.getQueueLink(link.getId());
					
					List <Link> toLinks = new ArrayList<Link>();
					for (SignalLane signalLane : signalGroupSetting.getSignalGroupDefinition().getToLanes()) {
						toLinks.add(this.getNode().getOutLinks().get(signalLane.getLinkId()));
					}

					for (PseudoLink pseudoLink : qLink.getNodePseudoLinks(toLinks)) {
						
						pseudoLink.setThisTimeStepIsGreen(true);
						
						while (!pseudoLink.flowQueueIsEmpty()) {
							Vehicle veh = pseudoLink.getFirstFromBuffer();
							if (!moveVehicleOverNode(veh, pseudoLink)) {
								break;
							}
						}
					}
				}

			}



		} else {

			//Node is NOT traffic light controlled
			
			// TODO Get Code from QueueNode's move

			for (Iterator<? extends Link> iter = this.getNode().getInLinks().values().iterator(); iter.hasNext();) {
				Link link = iter.next();
				
				QLink qLink = (QLink) this.queueNetwork.getQueueLink(link.getId());
				

				for (PseudoLink pseudoLink : qLink.getNodePseudoLinks()) {
					
					pseudoLink.setThisTimeStepIsGreen(true);
					
					while (!pseudoLink.flowQueueIsEmpty()) {
						Vehicle veh = pseudoLink.getFirstFromBuffer();
						if (!moveVehicleOverNode(veh, pseudoLink)) {
							break;
						}
					}
				}
			}

		}

	}

	/** Simple moveNode, Complex one can be found in {@link QueueNode}
	 * @param pseudoLink */
	public boolean moveVehicleOverNode(final Vehicle veh, PseudoLink pseudoLink) {
		// veh has to move over node
		Link nextLink = ((QVehicle)veh).chooseNextLink();
		
		if (nextLink != null) {
			QLink nextQLink = (QLink) this.queueNetwork.getQueueLink(nextLink.getId());
		
			if (nextQLink.hasSpace()) {
				pseudoLink.pollFirstFromBuffer();
				veh.incCurrentNode();
				nextQLink.add(veh);
				return true;
			}
			return false;
		}

		return true;
	}

}
