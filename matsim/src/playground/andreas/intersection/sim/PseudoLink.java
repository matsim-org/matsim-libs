package playground.andreas.intersection.sim;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.plans.Leg;

import playground.andreas.intersection.QControler;

public class PseudoLink {
	
	/** Logger */
	final private static Logger log = Logger.getLogger(QLink.class);
	
	/** Id of the real link, null if this is not the first/original <code>PseudoLink</code> */
	private QLink realLink = null;
	
	/** Hält alle echten Ziellinks, die von diesem Link erreichbar sind, meist 1-3 */
	private List<QLink> destLinks = new LinkedList<QLink>();
	
	/** Next PseudoLink upstream, null if first link */
	private PseudoLink fromLink = null;
	
	/** Contains next PseudoLinks downstream, null if last link */
	private List<PseudoLink> toLinks = new LinkedList<PseudoLink>();
	
	/** Meter counted from the end of the real link */
	private int meterFromLinkEnd = -1;
	
	/** The list of <code>QVehicles</code> that have not yet reached the end of the link according
	 * to the <code>freeSpeedTravelTime</code> of the <code>PseudoLink</code> */
	private Queue<QVehicle> storageQueue = new LinkedList<QVehicle>();

	/** Maximum number of vehicle to be stored on the link simultanously **/
	private double storageCapacity = Double.NaN;
	
	/** Buffer responsible for limiting the flow capacity, flowQueue is holding all vehicles
	 *  that are ready to cross the outgoing intersection **/
	private Queue<QVehicle> flowQueue = new LinkedList<QVehicle>();
	
	/** The number of vehicles able to leave the flowQueue in one time step (usually 1 sec). */
	private double flowCapacity = Double.NaN;
	
	/** Time needed to pass the link if empty */
	private double freeSpeedTravelTime = Double.NaN;
	
	/** parking queue includes all vehicles that do not have yet reached their
	 * start time, but will start at this link at some time */
	private final PriorityQueue<QVehicle> parkingQueue = new PriorityQueue<QVehicle>(30,
			new QVehicleDepartureTimeComparator());

	/** All vehicles from parkingQueue move to the waitingList as soon as their time
	 * has come. They are then filled into the storageQueue, depending on free space
	 * in the storageQueue */
	private final Queue<QVehicle> parkToLinkQueue = new LinkedList<QVehicle>();
	
	// Helper
	private double flowCapacityCeil = Double.NaN;
	private double flowCapacityFraction = Double.NaN;
	
	private double flowCapacityFractionalRest = 1.0;
	
	
	public PseudoLink(QLink originalLink) {
		this.realLink = originalLink;
	}

	public boolean recalculatePseudoLinkProperties(double length_m, int numberOfLanes, double freeSpeed_m_s, double flowCapacityOfOriginalLink){
		
		this.freeSpeedTravelTime = length_m / freeSpeed_m_s;

		this.flowCapacity = flowCapacityOfOriginalLink * SimulationTimer.getSimTickTime() * Gbl.getConfig().simulation().getFlowCapFactor();	

		this.flowCapacityCeil = (int) Math.ceil(this.flowCapacity);
		this.flowCapacityFraction = this.flowCapacity - (int) this.flowCapacity;

		this.storageCapacity = (length_m * numberOfLanes) / QNetworkLayer.CELL_LENGTH * Gbl.getConfig().simulation().getStorageCapFactor();

		this.storageCapacity = Math.max(this.storageCapacity, this.flowCapacityCeil);

		if (this.storageCapacity < this.freeSpeedTravelTime * this.flowCapacity) {
			this.storageCapacity = this.freeSpeedTravelTime * this.flowCapacity;
			return false;
			
		} else return true;
		
	}
	
	public void movePseudoLink(final double now){
		
		if (realLink != null){
			moveParkingQueueToParkToLinkQueue(now);
		}
		
		moveFlowQueueToNextPseudoLink();
		
		moveStorageQueueToFlowQueue(now);
		
		if (realLink != null){
			moveParkToLinkQueueToFlowQueue(now);
		}
		
	}
	
	private void moveStorageQueueToFlowQueue(final double now) {
		
		

		double maximumFlowCapacity = this.flowCapacity;
		
		QVehicle veh;
		while ((veh = this.storageQueue.peek()) != null) {
			if (veh.getDepartureTime_s() > now) {
				break;
			}

			if (veh.getDestinationLink().getId() == this.realLink.getId()) {
				
				QSim.getEvents().processEvent(new EventAgentArrival(now, veh.getDriverID(),
						veh.getCurrentLegNumber(), this.realLink.getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink));
				veh.reachActivity();				
				this.storageQueue.poll();
				continue;
			}

			if (!hasFlowQueueSpace()) {
				break;
			}

			if (maximumFlowCapacity >= 1.0) {
				maximumFlowCapacity--;
				addToFlowQueue(veh, now);
				this.storageQueue.poll();
				continue;
			
			} else if (this.flowCapacityFractionalRest >= 1.0) {
				this.flowCapacityFractionalRest--;
				addToFlowQueue(veh, now);
				this.storageQueue.poll();
				break;
			} else {
				break;
			}
		}

		if (this.flowCapacityFractionalRest < 1.0) {
			this.flowCapacityFractionalRest += this.flowCapacityFraction;
		}
		
	}

	private boolean hasFlowQueueSpace() {
		return (this.flowQueue.size() < this.flowCapacityCeil);
	}
	
	private void addToFlowQueue(final QVehicle veh, final double now) {
		this.flowQueue.add(veh);
		veh.setLastMovedTime(now);		
	}
	
	// TODO [an] Berücksichtigt lediglich die StorageQueue nicht aber die Fahrzeuge in der FlowQueue - Fehler bei David?
	public boolean hasSpace() {
		if (this.storageQueue.size() < this.storageCapacity) {
			return true;
		} else return false;
	}
	
	private void moveFlowQueueToNextPseudoLink(){
		
		while(!flowQueue.isEmpty()){
			QVehicle veh = this.flowQueue.peek();
			QLink nextLink = veh.chooseNextLink();
			
			if (nextLink != null) {
				for (PseudoLink toLink : toLinks) {
					for (QLink qLink : toLink.getDestLinks()) {
						if (qLink.equals(nextLink)){
							if (toLink.hasSpace()) {
								this.flowQueue.poll();
								toLink.addVehicle(veh);
							}	
						}
					}
				}
			}
		}
	}

	public List<QLink> getDestLinks() {
		return destLinks;
	}

	public void addDestLink(QLink destLink) {
		this.destLinks.add(destLink);
	}
	
	public void addVehicle(QVehicle vehicle){
		this.storageQueue.add(vehicle);
		vehicle.setDepartureTime_s((int) (SimulationTimer.getTime() + this.freeSpeedTravelTime));
	}	
	
	private void moveParkingQueueToParkToLinkQueue(final double now) {
		QVehicle veh;
		
		while ((veh = this.parkingQueue.peek()) != null) {
			
			if (veh.getDepartureTime_s() > now) {
				break;
			}

			veh.leaveActivity();

			QSim.getEvents().processEvent(new EventAgentDeparture(now, veh.getDriverID(),
					veh.getCurrentLegNumber(), this.realLink.getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink));
			
			Leg actLeg = veh.getCurrentLeg();

			if (actLeg.getRoute().getRoute().size() != 0) {
					this.parkToLinkQueue.add(veh);			
			}

			this.parkingQueue.poll();
		}
	}
	
	/** Move as many waiting cars to the link as it is possible
	 * @param now the current time */
	private void moveParkToLinkQueueToFlowQueue(final double now) {
		QVehicle veh;
		
		while ((veh = this.parkToLinkQueue.peek()) != null) {
			
			if (!hasFlowQueueSpace()) {
				break;
			}	
						
			addToFlowQueue(veh, now);

			QSim.getEvents().processEvent(new EventAgentWait2Link(now, veh.getDriverID(), 
					veh.getCurrentLegNumber(), this.realLink.getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink));

			this.parkToLinkQueue.poll(); // remove the just handled vehicle from parkToLinkQueue
		}
	}
	
	public void addVehicle2ParkingQueue(QVehicle veh) {
		parkingQueue.add(veh);
	}
	
	public Queue<QVehicle> getFlowQueue(){
		return flowQueue;
	}
		
}
