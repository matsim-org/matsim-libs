package playground.andreas.intersection.sim;

import java.util.Arrays;
import java.util.Comparator;

import org.matsim.events.AgentStuckEvent;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.Simulation;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.network.Link;
import org.matsim.network.Node;

public class QNode extends QueueNode{

	private boolean isSignalized = false;

	private boolean cacheIsInvalid = true;
	private QLink[] inLinksArrayCache = null;
	private QLink[] tempLinks = null;

	public QNode(Node n, QueueNetwork queueNetwork) {
		super(n,  queueNetwork);
	}
	
	public void setIsSignalizedTrue() {
		this.isSignalized = true;		
	}

	/** Simple moveNode, Complex one can be found in {@link QueueLink} */
	@Override
	public void moveNode(final double now) {

		if (this.isSignalized == true) {
			// Node is traffic light controlled
			for (Link link : this.getNode().getInLinks().values()) {
				QLink qLink = (QLink) this.queueNetwork.getQueueLink(link.getId());

				for (PseudoLink pseudoLink : qLink.getNodePseudoLinks()) {
					while (pseudoLink.firstVehCouldMove()) {
						QueueVehicle veh = pseudoLink.getFirstFromBuffer();
						if (!moveVehicleOverNode(veh, pseudoLink)) {
							break;
						}
					}
				}
			}

		} else {
			// Node is NOT traffic light controlled
			if (this.cacheIsInvalid) {
				buildCache();
			}
			
			int inLinksCounter = 0;
			double inLinksCapSum = 0.0;
			// Check all incoming links for buffered agents
			for (QLink link : this.inLinksArrayCache) {
				if (!link.bufferIsEmpty()) {
					this.tempLinks[inLinksCounter] = link;
					inLinksCounter++;
					inLinksCapSum += link.getLink().getCapacity(now);
				}
			}

			int auxCounter = 0;
			// randomize based on capacity
			while (auxCounter < inLinksCounter) {
				double rndNum = MatsimRandom.random.nextDouble() * inLinksCapSum;
				double selCap = 0.0;
				for (int i = 0; i < inLinksCounter; i++) {
					QLink link = this.tempLinks[i];
					if (link == null)
						continue;
					selCap += link.getLink().getCapacity(now);
					if (selCap >= rndNum) {
						auxCounter++;
						inLinksCapSum -= link.getLink().getCapacity(now);
						this.tempLinks[i] = null;
						for (PseudoLink pseudoLink : link.getNodePseudoLinks()) {
							while (!pseudoLink.flowQueueIsEmpty()) {
								QueueVehicle veh = pseudoLink.getFirstFromBuffer();
								if (!moveVehicleOverNode(veh, pseudoLink)) {
									break;
								}
							}
						}
						break;
					}
				}
			}
		}

	}

	/** Simple moveNode, Complex one can be found in {@link QueueNode} */
	public boolean moveVehicleOverNode(final QueueVehicle veh, PseudoLink pseudoLink) {
		double now = SimulationTimer.getTime();
		// veh has to move over node
		Link nextLink = veh.getDriver().chooseNextLink();
		if (nextLink != null) {
			QLink nextQLink = (QLink) this.queueNetwork.getQueueLink(nextLink.getId());
		
			if (nextQLink.hasSpace()) {
				pseudoLink.pollFirstFromBuffer();
				veh.getDriver().incCurrentNode();
				nextQLink.add(veh);
				return true;
			}
			// check if veh is stuck!

			if ((now - veh.getLastMovedTime()) > Simulation.getStuckTime()) {
				/* We just push the vehicle further after stucktime is over, regardless
				 * of if there is space on the next link or not.. optionally we let them
				 * die here, we have a config setting for that!
				 */
				if (removeStuckVehicle()) {
					pseudoLink.pollFirstFromBuffer();
					Simulation.decLiving();
					Simulation.incLost();
					QueueSimulation.getEvents().processEvent(
							new AgentStuckEvent(now, veh.getDriver().getPerson(), veh.getCurrentLink(), veh.getCurrentLeg()));
				}
				else {
					pseudoLink.pollFirstFromBuffer();
					veh.getDriver().incCurrentNode();
					nextQLink.add(veh);
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	private void buildCache() {
		this.inLinksArrayCache = new QLink[this.getNode().getInLinks().values().size()];
		int i = 0;
		for (Link l : this.getNode().getInLinks().values()) {
			this.inLinksArrayCache[i] = (QLink)this.queueNetwork.getLinks().get(l.getId());
			i++;
		}
		/* As the order of nodes has an influence on the simulation results,
		 * the nodes are sorted to avoid indeterministic simulations. dg[april08]
		 */
		Arrays.sort(this.inLinksArrayCache, new Comparator<QLink>() {
			public int compare(final QLink o1, final QLink o2) {
				return o1.getLink().getId().compareTo(o2.getLink().getId());
			}
		});
		this.tempLinks = new QLink[this.getNode().getInLinks().values().size()];
		this.cacheIsInvalid = false;
	}
	

	static boolean removeVehInitialized = false;

	static boolean removeVehicles = true;

	static boolean removeStuckVehicle() {
		if (removeVehInitialized) {
			return removeVehicles;
		}
		removeVehicles = Gbl.getConfig().simulation().removeStuckVehicles();
		removeVehInitialized = true;
		return removeVehicles;
	}
	
	
}