package playground.andreas.intersection.sim;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.events.EventLinkEnter;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class QLink extends Link {
	
	final private static Logger log = Logger.getLogger(QLink.class);
	private static int spaceCapWarningCount = 0;
	
	private PseudoLink originalLink = null;
	
	private ArrayList<PseudoLink> pseudoLinksList = new ArrayList<PseudoLink>();
	
	/** FreeLinkTravelTime */
	private double freeSpeedTT;
	
	protected double storageCapacity;
	protected double flowCapacityFractionalRest = 1.0;
	
	public QLink(NetworkLayer network, String id, Node from, Node to, String length, String freespeed, String capacity, String permlanes, String origid, String type) {
		super(network, id, from, to, length, freespeed, capacity, permlanes, origid, type);
		
		// Original LinkErstellen
		this.originalLink = new PseudoLink(this, true);
		// Configurieren
		if(! this.originalLink.recalculatePseudoLinkProperties(0., this.getLength(), this.getLanes(), this.getFreespeed(), this.getCapacity())) {
			
			if ( spaceCapWarningCount <=10 ) {
				log.warn("Link " + this.getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if ( spaceCapWarningCount == 10 ) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}
			
		}

		// Generate additional PseudoLinks for each possible direction
		boolean firstNodeLinkInitialized = false;
		
		for (Link link : this.getToNode().getOutLinks().values()) {
			
			double lengthOfNodeLinks_m = 45.0;
			int numberOfLanes_ = 1;
			double freeSpeed_m_s = this.getFreespeed();
			double flowCapacity_Veh_h = 2000.0;
			
			PseudoLink newNodePseudoLink;			
			
			// Shorten the orginal one and add an extension PseudoLink
			if(!firstNodeLinkInitialized){
				
				newNodePseudoLink = new PseudoLink(this, false);
				
				newNodePseudoLink.setFromLink(originalLink);
				newNodePseudoLink.addDestLink(link);
				originalLink.getToLinks().add(newNodePseudoLink);
				originalLink.addDestLink(link);			
				
				newNodePseudoLink.recalculatePseudoLinkProperties(0, lengthOfNodeLinks_m, numberOfLanes_, freeSpeed_m_s, flowCapacity_Veh_h);
				this.originalLink.recalculatePseudoLinkProperties(lengthOfNodeLinks_m, this.getLength() - lengthOfNodeLinks_m, this.getLanes(), this.getFreespeed(), this.getCapacity());
			
				pseudoLinksList.add(newNodePseudoLink);
				firstNodeLinkInitialized = true;
				
			} else {
				
				// Now we have the original link and one extension pseudo Link
				// therefore add additional extension links for the rest of the outLinks
				
				// Check, if the new extension link is in proximity of an old one's staring point
				if (lengthOfNodeLinks_m - originalLink.getMeterFromLinkEnd() > 15.0){
					// It is not
					
					System.err.println("Not Implemented yet: Every PseudoNode in 15m proximity of an old PseudoNode will be ajected to the old one");
					
					// Insert new one...
					// Fix Pointer...
					// Adjust SC, SQ...
					// Generate new NodePseudoLink
					// Fix Pointer...
					// Adjust SC, SQ...
					
				}else{
					// It is
					// New NodePseudoLink will start at originalLink
					
					newNodePseudoLink = new PseudoLink(this, false);
					
					newNodePseudoLink.setFromLink(originalLink);
					newNodePseudoLink.addDestLink(link);
					originalLink.getToLinks().add(newNodePseudoLink);
					originalLink.addDestLink(link);
					
					// Only need to fix properties of new link. Original link hasn't changed
					newNodePseudoLink.recalculatePseudoLinkProperties(0, lengthOfNodeLinks_m, numberOfLanes_, freeSpeed_m_s, flowCapacity_Veh_h);
					
					pseudoLinksList.add(newNodePseudoLink);
					
				}
				
				
			}
			
			
		}
		
		pseudoLinksList.add(originalLink);
	}
	
	/** Adds a vehicle to the parkingQueue */
	public void addVehicle2ParkingQueue(QVehicle veh) {
		originalLink.addVehicle2ParkingQueue(veh);	
	}

	public double getFreeSpeedTT() {
		return freeSpeedTT;
	}
	
	/** Called by QNetworkLayer */
	boolean moveLink(final double now) {
		
		for (PseudoLink pseudoLink : pseudoLinksList) {
			pseudoLink.movePseudoLink(now);
		}		
		return true ;
	}
	
	public boolean hasSpace() {
		return originalLink.hasSpace();		
	}
	 
	public void add(final QVehicle veh) {
		veh.setCurrentLink(this);
		this.originalLink.addVehicle(veh);
		QSim.getEvents().processEvent(new EventLinkEnter(SimulationTimer.getTime(), veh.getDriverID(),
				veh.getCurrentLegNumber(), this.getId().toString(), veh.getDriver(), this));
	}
	
	public List<PseudoLink> getNodePseudoLinks(){
		
		List<PseudoLink> nodePseudoLinksList = new LinkedList<PseudoLink>();
		
		for (PseudoLink pseudoLink : pseudoLinksList) {
			if (pseudoLink.getMeterFromLinkEnd() == 0){
				nodePseudoLinksList.add(pseudoLink);
			}			
		}		
		return nodePseudoLinksList;		
	}
}
