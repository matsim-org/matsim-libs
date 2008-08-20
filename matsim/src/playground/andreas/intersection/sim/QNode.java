package playground.andreas.intersection.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.matsim.gbl.MatsimRandom;
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
	

	private boolean cacheIsInvalid = true;

	private QLink[] inLinksArrayCache = null;

	private QLink[] tempLinks = null;

	private QLink[] auxLinks = null;
	
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
						this.auxLinks[auxCounter] = link;
						auxCounter++;
						inLinksCapSum -= link.getLink().getCapacity(now);
						this.tempLinks[i] = null;
						break;
					}
				}
			}
			
			for (int i = 0; i < auxCounter; i++) {
				QLink qlink = this.auxLinks[i];
				for (PseudoLink pseudoLink : qlink.getNodePseudoLinks()) {
					
//					pseudoLink.setThisTimeStepIsGreen(true);
					
					while (!pseudoLink.flowQueueIsEmpty()) {
						Vehicle veh = pseudoLink.getFirstFromBuffer();
						if (!moveVehicleOverNode(veh, pseudoLink)) {
							break;
						}
					}
				}
			}
			
			
//			for (Iterator<? extends Link> iter = this.getNode().getInLinks().values().iterator(); iter.hasNext();) {
//				Link link = iter.next();
//				
//				QLink qLink = (QLink) this.queueNetwork.getQueueLink(link.getId());
//				
//
//				for (PseudoLink pseudoLink : qLink.getNodePseudoLinks()) {
//					
//					pseudoLink.setThisTimeStepIsGreen(true);
//					
//					while (!pseudoLink.flowQueueIsEmpty()) {
//						Vehicle veh = pseudoLink.getFirstFromBuffer();
//						if (!moveVehicleOverNode(veh, pseudoLink)) {
//							break;
//						}
//					}
//				}
//			}
//
		}

	}

	/** Simple moveNode, Complex one can be found in {@link QueueNode}
	 * @param pseudoLink */
	public boolean moveVehicleOverNode(final Vehicle veh, PseudoLink pseudoLink) {
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
			return false;
		}

		return true;
	}
	
	private void buildCache() {
		this.inLinksArrayCache = new QLink[this.getNode().getInLinks().values()
				.size()];
		int i = 0;
		for (Link l : this.getNode().getInLinks().values()) {
			this.inLinksArrayCache[i] = (QLink)this.queueNetwork.getLinks().get(
					l.getId());
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
		this.auxLinks = new QLink[this.getNode().getInLinks().values().size()];
		this.cacheIsInvalid = false;
	}

}
