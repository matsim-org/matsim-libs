package playground.andreas.intersection.sim;

import java.util.Iterator;

import org.matsim.mobsim.QueueLink;
import org.matsim.network.Node;
import org.matsim.trafficlights.data.SignalGroupSettings;

import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QNode extends Node{
	
	private SignalSystemControlerImpl myNodeTrafficLightControler;
	
	public QNode(String id, String x, String y, String type) {
		super(id, x, y, type);		
	}
	
	public void setSignalSystemControler(SignalSystemControlerImpl nodeControler){
		this.myNodeTrafficLightControler = nodeControler;
	}
	
	public SignalSystemControlerImpl getMyNodeTrafficLightControler() {
		return myNodeTrafficLightControler;
	}

	/** Simple moveNode, Complex one can be found in {@link QueueLink} */
	public void moveNode(final double now) {
		
		if(myNodeTrafficLightControler != null){
			
			// Node is traffic light controlled
			
			SignalGroupSettings[] greenSignalGroups = myNodeTrafficLightControler.getGreenInLinks(now);
			
			if (greenSignalGroups.length != 0){
				
				for (int i = 0; i < greenSignalGroups.length; i++) {
					SignalGroupSettings signalGroupSetting = greenSignalGroups[i];
					
					QLink link = (QLink) this.inlinks.get((signalGroupSetting.getSignalGroupDefinition().getLinkId()));
					
					for (PseudoLink pseudoLink : link.getNodePseudoLinks()) {
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
			
			for (Iterator iter = this.inlinks.values().iterator(); iter.hasNext();) {
				QLink link = (QLink) iter.next();
				
				for (PseudoLink pseudoLink : link.getNodePseudoLinks()) {
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
