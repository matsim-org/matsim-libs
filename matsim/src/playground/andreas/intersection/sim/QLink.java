package playground.andreas.intersection.sim;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.gbl.Gbl;

import org.matsim.mobsim.QueueNode;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Leg;

public class QLink extends Link {
	
	final private static Logger log = Logger.getLogger(QLink.class);
	private static int spaceCapWarningCount = 0;

	/** FreeLinkTravelTime */
	private double freeLinkTT;
	/** The number of vehicles able to leave the buffer in one time step (usually 1 sec). */
	private double simulatedFlowCapacity;
	
	private int timeCapCeil; // optimization, cache Math.ceil(timeCap)
	private double timeCapFraction; // optimization, cache timeCap - (int)timeCap
	
	protected double storageCapacity;
	protected double buffercap_accumulate = 1.0;
	
	/** parking list includes all vehicle that do not have yet reached their
	 * start time, but will start at this link at some time */
	private final PriorityQueue<QVehicle> parkingQueue = new PriorityQueue<QVehicle>(30,
			new QVehicleDepartureTimeComparator());

	/** All vehicles from parkingQueue move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue */
	private final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();

	/** The list of vehicles that have not yet reached the end of the link according
	 * to the free travel speed of the link */
	private final Queue<QVehicle> vehQueue = new LinkedList<QVehicle>();

	/** buffer is holding all vehicles that are ready to cross the outgoing intersection */
	private final Queue<QVehicle> buffer = new LinkedList<QVehicle>();
		
	public QLink(NetworkLayer network, String id, Node from, Node to, String length, String freespeed, String capacity, String permlanes, String origid, String type) {
		super(network, id, from, to, length, freespeed, capacity, permlanes, origid, type);
		
		this.freeLinkTT = this.getLength() / this.getFreespeed();
			
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor 
		double flowCapFactor = Gbl.getConfig().simulation().getFlowCapFactor();
		// multiplying capacity from file by simTickCapFactor **and** flowCapFactor:
		this.simulatedFlowCapacity = this.getFlowCapacity() * SimulationTimer.getSimTickTime() * flowCapFactor;
			
		recalcCapacity();		
	}

	/** Copied from David's Queuelink */
	private void recalcCapacity() {
		/* network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor */
		//TODO [an] What is storage cap factor
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		// also computing the ceiling of the capacity:
		this.timeCapCeil = (int) Math.ceil(this.simulatedFlowCapacity);

		// ... and also the fractional part of timeCap
		this.timeCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		this.storageCapacity = (this.getLength() * this.getLanes()) / NetworkLayer.CELL_LENGTH * storageCapFactor;

		/* storage capacity needs to be at least enough to handle the cap_per_time_step: */
		this.storageCapacity = Math.max(this.storageCapacity, this.timeCapCeil);

		/* If speed on link is relatively slow, then we need MORE cells than the above spaceCap to handle the flowCap. Example:
		 * Assume freeSpeedTravelTime (aka freeLinkTT) is 2 seconds. Than I need the spaceCap TWO times the flowCap to
		 * handle the flowCap. */
		if (this.storageCapacity < this.freeLinkTT * this.simulatedFlowCapacity) {
			if ( spaceCapWarningCount <=10 ) {
				log.warn("Link " + this.getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if ( spaceCapWarningCount == 10 ) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}
			this.storageCapacity = this.freeLinkTT * this.simulatedFlowCapacity;
		}
	}
	
	/** Adds a vehicle to the parkingQueue */
	public void addVehicle2ParkingQueue(QVehicle veh) {
		parkingQueue.add(veh);
	}

	public double getFreeLinkTT() {
		return freeLinkTT;
	}
	
	/** Called by QNetworkLayer */
	boolean moveLink(final double now) {
		// move vehicles from parking into waitingQueue if applicable
		moveParkToWait(now);
		// move vehicles from link to buffer
		moveLinkToBuffer(now);
		// move vehicles from waitingQueue into buffer if possible
		moveWaitToBuffer(now);

		return true ;
	}

	/** Moves those vehicles, whose departure time has come, from the parking list to the wait list, 
	 *  from where they can later enter the link.
	 * @param now the current time */
	private void moveParkToWait(final double now) {
		QVehicle veh;
		
		while ((veh = this.parkingQueue.peek()) != null) {
			
			if (veh.getDepartureTime_s() > now) {
				break;
			}

			// Need to inform the veh that it now leaves its activity.
			veh.leaveActivity();

			// Generate departure event
			QSim.getEvents().processEvent(new EventAgentDeparture(now, veh.getDriverID(),
					veh.getCurrentLegNumber(), getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this));
			
			Leg actLeg = veh.getCurrentLeg();

			if (actLeg.getRoute().getRoute().size() != 0) {
					this.waitingList.add(veh);			
			}

			/* Remove vehicle from parkingList
			 * Do that as the last step to guarantee that the link is ACTIVE all the time
			 * because veh.reinitVeh() calls addParking which might come to the false conclusion,
			 * that this link needs to be activated, as parkingQueue is empty */
			this.parkingQueue.poll();
		}
	}

	/** Move as many waiting cars to the link as it is possible
	 * @param now the current time */
	private void moveWaitToBuffer(final double now) {
		QVehicle veh;
		
		while ((veh = this.waitingList.peek()) != null) {
			
			if (!hasBufferSpace()) {
				break;
			}	
						
			addToBuffer(veh, now);

			QSim.getEvents().processEvent(new EventAgentWait2Link(now, veh.getDriverID(), 
					veh.getCurrentLegNumber(), getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this));

			this.waitingList.poll(); // remove the just handled vehicle from waitingList
		}
	}
	
	/** @return <code>true</code> if there are less vehicles in buffer than the flowCapacity's ceil */
	private boolean hasBufferSpace() {
		return (this.buffer.size() < this.timeCapCeil);
	}
	
	private void addToBuffer(final QVehicle veh, final double now) {
		this.buffer.add(veh);
		veh.setLastMovedTime(now);		
	}

	/** Move vehicles from link to buffer, according to buffer capacity and departure time of vehicle.
	 *  @param now The current time. */
	private void moveLinkToBuffer(final double now) {
		// move items if possible
		double max_buffercap = this.simulatedFlowCapacity;

		QVehicle veh;
		while ((veh = this.vehQueue.peek()) != null) {
			if (veh.getDepartureTime_s() > now) {
				break;
			}

			// Check if veh has reached destination:
			if (veh.getDestinationLink().getId() == this.getId()) {
				processVehicleArrival(now, veh);

				// remove _after_ processing the arrival to keep link active
				this.vehQueue.poll();
				continue;
			}

			// is there still room left in the buffer, or is it overcrowded from the last time steps?
			if (!hasBufferSpace()) {
				break;
			}

			if (max_buffercap >= 1.0) {
				max_buffercap--;
				addToBuffer(veh, now);
				this.vehQueue.poll();
				continue;
			
			} else if (this.buffercap_accumulate >= 1.0) {
				this.buffercap_accumulate--;
				addToBuffer(veh, now);
				this.vehQueue.poll();
				break;
			} else {
				break;
			}
		}

		/* if there is an fractional piece of bufferCapacity left over
		 * leave it for the next iteration so it might accumulate to a whole agent.
		 */
		if (this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.timeCapFraction;
		}
	}
	
	private void processVehicleArrival(final double now, final QVehicle veh ) {
		QSim.getEvents().processEvent(new EventAgentArrival(now, veh.getDriverID(),
				veh.getCurrentLegNumber(), getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this));
		// Need to inform the veh that it now reached its destination.
		veh.reachActivity();
	}

	boolean bufferIsEmpty() {
		return this.buffer.isEmpty();
	}
	
	/** @return <code>true</code> if there are less vehicles in buffer + vehQueue (= the whole link),
	 * than there is space for vehicles. */
	public boolean hasSpace() {
		if (this.vehQueue.size() < getSpaceCap()) {
			return true;
		} else return false;
	}

	/** @return Returns the maxCap. */
	public double getSpaceCap() {
		return this.storageCapacity;
	}
	
	QVehicle getFirstFromBuffer() {
		return this.buffer.peek();
	}
	
	QVehicle pollFirstFromBuffer() {
		double now = SimulationTimer.getTime();
		QVehicle veh = this.buffer.poll();
//		QVehicle v2 = this.buffer.peek();
//		if (v2 != null) {
//			v2.setLastMovedTime(now);
//		}

		QSim.getEvents().processEvent(new EventLinkLeave(now, veh.getDriverID(), veh.getCurrentLegNumber(),
						this.getId().toString(), veh.getDriver(), this));

		return veh;
	}
	 
	 /** Adds a vehicle to the link, called by {@link QueueNode#moveVehicleOverNode(Vehicle, double)}.
	 * @param veh the vehicle */
	public void add(final QVehicle veh) {
		double now = SimulationTimer.getTime();

		veh.setCurrentLink(this);
		this.vehQueue.add(veh);
		veh.setDepartureTime_s((int) (now + this.freeLinkTT));
		QSim.getEvents().processEvent(new EventLinkEnter(now, veh.getDriverID(),
				veh.getCurrentLegNumber(), this.getId().toString(), veh.getDriver(), this));
	}
}
