package playground.andreas.intersection.sim;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class QLink extends Link {
	
	final private static Logger log = Logger.getLogger(QLink.class);
	private static int spaceCapWarningCount = 0;
	
	private PseudoLink originalLink = null;

	/** FreeLinkTravelTime */
	private double freeSpeedTT;
	
	protected double storageCapacity;
	protected double flowCapacityFractionalRest = 1.0;
	
	private final Queue<QVehicle> flowQueue = new LinkedList<QVehicle>();
	
	// checked okay
	public QLink(NetworkLayer network, String id, Node from, Node to, String length, String freespeed, String capacity, String permlanes, String origid, String type) {
		super(network, id, from, to, length, freespeed, capacity, permlanes, origid, type);
		
		this.originalLink = new PseudoLink(this);
		
		if(! this.originalLink.recalculatePseudoLinkProperties(this.getLength(), this.getLanes(), this.getFreespeed(), this.getCapacity())) {
			
			if ( spaceCapWarningCount <=10 ) {
				log.warn("Link " + this.getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if ( spaceCapWarningCount == 10 ) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}
			
		}

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
				
		originalLink.movePseudoLink(now);
		return true ;
	}

	boolean flowQueueIsEmpty() {
		// TODO [an] Hier muss zwischen den einzelnen 0m-Links unterschieden werden
		// also flowQueueIsEmpty(destLink) liefert yes/no;
		return this.flowQueue.isEmpty();
	}
	
	public boolean hasSpace() {
		return originalLink.hasSpace();		
	}
	
	QVehicle getFirstFromBuffer() {
		// TODO [an] Hier muss zwischen den einzelnen 0m-Links unterschieden werden
		// also getFirstFromBuffer(destLink) liefert veh von destLink-PseudoLink;
		return this.flowQueue.peek();
	}
	
	QVehicle pollFirstFromBuffer() {
		// TODO [an] Hier muss zwischen den einzelnen 0m-Links unterschieden werden
		// also pollFirstFromBuffer(destLink) liefert veh von destLink-PseudoLink;
		double now = SimulationTimer.getTime();
		QVehicle veh = this.flowQueue.poll();
//		QVehicle v2 = this.buffer.peek();
//		if (v2 != null) {
//			v2.setLastMovedTime(now);
//		}

		QSim.getEvents().processEvent(new EventLinkLeave(now, veh.getDriverID(), veh.getCurrentLegNumber(),
						this.getId().toString(), veh.getDriver(), this));

		return veh;
	}
	 
	public void add(final QVehicle veh) {
		veh.setCurrentLink(this);
		this.originalLink.addVehicle(veh);
		QSim.getEvents().processEvent(new EventLinkEnter(SimulationTimer.getTime(), veh.getDriverID(),
				veh.getCurrentLegNumber(), this.getId().toString(), veh.getDriver(), this));
	}
}
