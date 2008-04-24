package playground.andreas.intersection.sim;

import java.util.Iterator;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.trafficlights.data.SignalGroupSettings;

import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QNode extends QueueNode{

	private SignalSystemControlerImpl myNodeTrafficLightControler;
	
	public QNode(Node n, QueueNetworkLayer queueNetworkLayer) {
		super(n,  queueNetworkLayer);
	}

	public void setSignalSystemControler(SignalSystemControlerImpl nodeControler){
		this.myNodeTrafficLightControler = nodeControler;
	}

	public SignalSystemControlerImpl getMyNodeTrafficLightControler() {
		return this.myNodeTrafficLightControler;
	}

	/** Simple moveNode, Complex one can be found in {@link QueueLink} */
	public void moveNode(final double now) {

		if(this.myNodeTrafficLightControler != null){

			// Node is traffic light controlled

			SignalGroupSettings[] greenSignalGroups = this.myNodeTrafficLightControler.getGreenInLinks(now);

			if (greenSignalGroups.length != 0){

				for (int i = 0; i < greenSignalGroups.length; i++) {
					SignalGroupSettings signalGroupSetting = greenSignalGroups[i];

					Link link = (Link) this.getNode().getInLinks().get((signalGroupSetting.getSignalGroupDefinition().getLinkId()));
					
					QLink qLink = (QLink) queueNetworkLayer.getQueueLink(link.getId());

					for (PseudoLink pseudoLink : qLink.getNodePseudoLinks()) {
						while (!pseudoLink.flowQueueIsEmpty()) {
							QVehicle veh = pseudoLink.getFirstFromBuffer();
							if (!moveVehicleOverNode(veh, now, pseudoLink)) {
								break;
							}
						}
					}
				}

			}



		} else {

			//Node is NOT traffic light controlled

			for (Iterator iter = this.getNode().getInLinks().values().iterator(); iter.hasNext();) {
				Link link = (Link) iter.next();
				
				QLink qLink = (QLink) queueNetworkLayer.getQueueLink(link.getId());
				

				for (PseudoLink pseudoLink : qLink.getNodePseudoLinks()) {
					while (!pseudoLink.flowQueueIsEmpty()) {
						QVehicle veh = pseudoLink.getFirstFromBuffer();
						if (!moveVehicleOverNode(veh, now, pseudoLink)) {
							break;
						}
					}
				}
			}

		}

	}

	/** Simple moveNode, Complex one can be found in {@link QueueLink}
	 * @param pseudoLink */
	public boolean moveVehicleOverNode(final QVehicle veh, final double now, PseudoLink pseudoLink) {
		// veh has to move over node
		QLink nextLink = veh.chooseNextLink();

		if (nextLink != null) {
			if (nextLink.hasSpace()) {
				pseudoLink.pollFirstFromBuffer();
				veh.incCurrentNode();
				nextLink.add(veh);
				return true;
			}
			return false;
		}

		return true;
	}

}
