package playground.andreas.intersection.sim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.mobsim.Simulation;
import org.matsim.network.Node;

public class QNode extends Node{
		
	private QLink tempLinks[];
	private QLink auxLinks[];
	/** Needs to be set to true if tempLinks[] and auxLinks[] were not initialized. */
	private boolean cacheIsInvalid;
	
	public QNode(String id, String x, String y, String type) {
		super(id, x, y, type);		
	}
	
	/** Is overwritten, cause of setting the cache */
	@Override
	public boolean addInLink(final BasicLinkI inlink) {
		this.cacheIsInvalid = true;
		return super.addInLink(inlink);
	}
	
	/** Initializes tempLinks[] and auxLinks[] */
	private void buildCache() {
		this.tempLinks = new QLink[this.inlinks.size()];
		this.auxLinks = new QLink[this.inlinks.size()];
		this.cacheIsInvalid = true;
	}
	
	/**
	 * Moves vehicles from the inlinks' buffer to the outlinks where possible.<br>
	 * The inLinks are randomly chosen, and for each link all vehicles in the
	 * buffer are moved to their desired outLink as long as there is space. If
	 * the front most vehicle in a buffer cannot move across the node because
	 * there is no free space on its destination link, the work on this inLink is
	 * finished and the next inLink's buffer is handled (this means, that at the
	 * node, all links have only like one lane, and there are no separate lanes
	 * for the different outLinks. Thus if the front most vehicle cannot drive
	 * further, all other vehicles behind must wait, too, even if their links
	 * would be free).
	 *
	 * @param now The current time in seconds from midnight.
	 */
	public void moveNode(final double now) {
		/* called by the framework, do all necessary action for node movement here */
		
		if (this.cacheIsInvalid) {
			buildCache();
		}
		
		int tempCounter = 0;
		double tempCap = 0.0;
		// Check all incoming links for buffered agents
		
		for (Iterator iter = this.inlinks.values().iterator(); iter.hasNext();) {
			QLink link = (QLink) iter.next();
				
			if (!link.bufferIsEmpty()) {
				this.tempLinks[tempCounter] = link;
				tempCounter++;
				tempCap += link.getCapacity();
			}
		}

		if (tempCounter == 0) {
			return; // Nothing to do
		}

		int auxCounter = 0;
		// randomize based on capacity
		while (auxCounter < tempCounter) {
			double rndNum = Gbl.random.nextDouble() * tempCap;
			double selCap = 0.0;
			for (int i = 0; i < tempCounter; i++) {
				QLink link = this.tempLinks[i];
				if (link == null) continue;
				selCap += link.getCapacity();
				if ( selCap >= rndNum ) {
					this.auxLinks[auxCounter] = link;
					auxCounter++;
					tempCap -= link.getCapacity();
					this.tempLinks[i] = null;
					break ;
				}
			}
		}

		for (int i = 0; i < auxCounter; i++) {
			QLink link = this.auxLinks[i];
			// Move agents/vehicle data to next link
			while (!link.bufferIsEmpty()) {
				QVehicle veh = link.getFirstFromBuffer();
				if (!moveVehicleOverNode(veh, now)) {
					break;
				}
			}
		}
	}
	
	public boolean moveVehicleOverNode(final QVehicle veh, final double now) {
		// veh has to move over node
		QLink nextLink = veh.chooseNextLink();

		if (nextLink != null) {
			if (nextLink.hasSpace()) {
				veh.getCurrentLink().popFirstFromBuffer();
				veh.incCurrentNode();
				nextLink.add(veh);
				return true;
			}			
			return false;
		}

		// --> nextLink == null
		veh.getCurrentLink().popFirstFromBuffer();
		Simulation.decLiving();
		Simulation.incLost();
		Logger.getLogger(QNode.class).error("Agent has no or wrong route! agentId=" + veh.getDriverID() 
				+ " currentLegNumber=" + veh.getCurrentLegNumber() 
				+ " currentLink=" + veh.getCurrentLink().getId().toString()
				+ "The agent is removed from the simulation.");
		return true;
	}
	
}
