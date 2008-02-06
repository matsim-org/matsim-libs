package playground.andreas.intersection.sim;

import java.util.Iterator;

import org.matsim.mobsim.QueueLink;
import org.matsim.network.Node;

public class QNode extends Node{
	
	public QNode(String id, String x, String y, String type) {
		super(id, x, y, type);		
	}

	/** Simple moveNode, Complex one can be found in {@link QueueLink} */
	public void moveNode(final double now) {
				
		for (Iterator iter = this.inlinks.values().iterator(); iter.hasNext();) {
			QLink link = (QLink) iter.next();
				
			while (!link.flowQueueIsEmpty()) {
				QVehicle veh = link.getFirstFromBuffer();
				if (!moveVehicleOverNode(veh, now)) {
					break;
				}
			}
		}
	}
	
	/** Simple moveNode, Complex one can be found in {@link QueueLink} */
	public boolean moveVehicleOverNode(final QVehicle veh, final double now) {
		// veh has to move over node
		QLink nextLink = veh.chooseNextLink();

		if (nextLink != null) {
			if (nextLink.hasSpace()) {
				veh.getCurrentLink().pollFirstFromBuffer();
				veh.incCurrentNode();
				nextLink.add(veh);
				return true;
			}			
			return false;
		}

		return true;
	}
	
}
