package playground.andreas.intersection.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.basic.lightsignalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.v01.Id;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.mobsim.queuesim.VehicleDepartureTimeComparator;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

public class PseudoLink implements Comparable<PseudoLink> {

	/** Logger */
	@SuppressWarnings("unused")
	final private static Logger log = Logger.getLogger(QLink.class);

	/** Id of the real link, null if this is not the first/original <code>PseudoLink</code> */
	private QLink realLink = null;

	private boolean amIOriginalLink = false;
	private Id laneLinkIdSpecifiedInFile;
	private ArrayList<BasicLightSignalGroupDefinition> sgsOnThisSubLink = null;

	/** Haelt alle echten Ziellinks, die von diesem Link erreichbar sind, meist 1-3 */
	private List<Link> destLinks = new LinkedList<Link>();

	/** Contains next PseudoLinks downstream, null if last link */
	private List<PseudoLink> toLinks = new LinkedList<PseudoLink>();

	/** Meter counted from the end of the real link */
	private double meterFromLinkEnd = -1;

	/**
	 * The list of <code>QVehicles</code> that have not yet reached the end of
	 * the link according to the <code>freeSpeedTravelTime</code> of the
	 * <code>PseudoLink</code>
	 */
	private Queue<Vehicle> storageQueue = new LinkedList<Vehicle>();

	/** Maximum number of vehicle to be stored on the link simultanously */
	private double storageCapacity = Double.NaN;

	/** Buffer responsible for limiting the flow capacity, flowQueue is holding
	 * all vehicles that are ready to cross the outgoing intersection */
	private Queue<Vehicle> flowQueue = new LinkedList<Vehicle>();

	/** The number of vehicles able to leave the flowQueue in one time step (usually 1 sec). */
	private double flowCapacity = Double.NaN;

	/** Time needed to pass the link if empty */
	private double freeSpeedTravelTime = Double.NaN;

	/**
	 * parking queue includes all vehicles that do not have yet reached their
	 * start time, but will start at this link at some time
	 */
	private final PriorityQueue<Vehicle> parkingQueue = new PriorityQueue<Vehicle>(30,
			new VehicleDepartureTimeComparator());

	/**
	 * All vehicles from parkingQueue move to the waitingList as soon as their
	 * time has come. They are then filled into the storageQueue, depending on
	 * free space in the storageQueue
	 */
	private final Queue<Vehicle> parkToLinkQueue = new LinkedList<Vehicle>();

	// Helper
	private double flowCapacityCeil = Double.NaN;
	private double flowCapacityFraction = Double.NaN;
	private double flowCapacityFractionalRest = 1.0;
	private double maximumFlowCapacity = 0.;
	private boolean thisTimeStepIsGreen = false;

	/** For Visualization only */
	int visualizerLane = 1;
	double length_m = -1;

	public PseudoLink(QLink originalLink, boolean amIOriginalLink, Id laneLinkIdSpecifiedInFile) {
		this.realLink = originalLink;
		this.amIOriginalLink = amIOriginalLink;
		this.laneLinkIdSpecifiedInFile = laneLinkIdSpecifiedInFile;
	}

	public boolean recalculatePseudoLinkProperties(double meterFromLinkEnd_m, double lengthOfPseudoLink_m,
			int numberOfLanes, double freeSpeed_m_s, double averageSimulatedFlowCapacityPerLane_Veh_s,
			double effectiveCellSize) {
		
		if(lengthOfPseudoLink_m < 15){
			log.warn("Length of one of link " + this.realLink.getLink().getId() + " sublinks is less than 15m." +
					" Will enlarge length to 15m, since I need at least additional 15m space to store 2 vehicles" +
					" at the original link.");
			this.length_m = 15.0;
		} else {
			this.length_m = lengthOfPseudoLink_m;
		}
		
		this.meterFromLinkEnd = meterFromLinkEnd_m;
		this.freeSpeedTravelTime = this.length_m / freeSpeed_m_s;

		this.flowCapacity = numberOfLanes * averageSimulatedFlowCapacityPerLane_Veh_s
				* SimulationTimer.getSimTickTime() * Gbl.getConfig().simulation().getFlowCapFactor();

		this.flowCapacityCeil = (int) Math.ceil(this.flowCapacity);
		this.flowCapacityFraction = this.flowCapacity - (int) this.flowCapacity;
		this.storageCapacity = (this.length_m * numberOfLanes) / 
								effectiveCellSize * Gbl.getConfig().simulation().getStorageCapFactor();
		this.storageCapacity = Math.max(this.storageCapacity, this.flowCapacityCeil);

		this.flowCapacityFractionalRest = (this.flowCapacityFraction == 0.0 ? 0.0 : 1.0);

		if (this.storageCapacity < this.freeSpeedTravelTime * this.flowCapacity) {
			this.storageCapacity = this.freeSpeedTravelTime * this.flowCapacity;
			return false;
		}
		return true;
	}
	
	public void addLightSignalGroupDefinition(BasicLightSignalGroupDefinition basicLightSignalGroupDefinition) {
		for (Id laneId : basicLightSignalGroupDefinition.getLaneIds()) {
			if (this.laneLinkIdSpecifiedInFile.equals(laneId)) {
				if (this.sgsOnThisSubLink == null) {
					this.sgsOnThisSubLink = new ArrayList<BasicLightSignalGroupDefinition>();
				}
				this.sgsOnThisSubLink.add(basicLightSignalGroupDefinition);
			}
		}
	}

	public boolean firstVehCouldMove() {
		if (this.sgsOnThisSubLink == null) {
			log.fatal("This should never happen, since every LaneLink at a signalized" +
					" intersection should have at least one signal(group)");
		}

		boolean firstVehInQueueCouldMove = false;

		for (BasicLightSignalGroupDefinition signalGroup : this.sgsOnThisSubLink) {
			boolean sgIsGreen = signalGroup.isGreen();
			if (sgIsGreen) {
				this.setThisTimeStepIsGreen(true);
			}

			Vehicle veh = this.getFirstFromBuffer();
			if (veh != null) {
				// Necessary signal group is green
				if (sgIsGreen && signalGroup.getToLinkIds().contains(
								this.getFirstFromBuffer().getDriver().chooseNextLink().getId())) {
					return firstVehInQueueCouldMove = true;
				} 
				// Its a Vehicle performing an UTurn
				else if (sgIsGreen && this.getFirstFromBuffer().getDriver().chooseNextLink().getToNode().equals(this.realLink.getLink().getFromNode())){
					return firstVehInQueueCouldMove = true;
				}
			}
		}
		return firstVehInQueueCouldMove;
	}

	public void setThisTimeStepIsGreen(boolean isGreen) {
		this.thisTimeStepIsGreen = isGreen;
	}

	public void movePseudoLink(final double now) {
		if (this.meterFromLinkEnd == 0.0) {
			if (this.thisTimeStepIsGreen == true) {
				updateBufferCapacity();
			}
		} else {
			updateBufferCapacity();
		}

		this.maximumFlowCapacity = this.flowCapacity;

		if (this.amIOriginalLink) {
			moveParkingQueueToParkToLinkQueue(now);
		}
		moveStorageQueueToFlowQueue(now);
		moveFlowQueueToNextPseudoLink();
		if (this.amIOriginalLink) {
			moveParkToLinkQueueToFlowQueue(now);
		}

		this.setThisTimeStepIsGreen(false);
	}

	private void moveStorageQueueToFlowQueue(final double now) {
		Vehicle veh;
		while ((veh = this.storageQueue.peek()) != null) {
			if (Math.floor(veh.getDepartureTime_s()) > now) {
				break;
			}

			if (veh.getDriver().getDestinationLink().getId() == this.realLink.getLink().getId()) {
				QueueSimulation.getEvents().processEvent(new AgentArrivalEvent(now, veh.getDriver().getPerson(),
								this.realLink.getLink(), veh.getCurrentLeg()));
				veh.getDriver().reachActivity(now, this.realLink);
				this.storageQueue.poll();
				continue;
			}

			if (!hasFlowQueueSpace()) {
				break;
			}

			if (this.maximumFlowCapacity >= 1.0) {
				addToFlowQueue(veh, now);
				this.storageQueue.poll();
				continue;

			} else if (this.flowCapacityFractionalRest >= 1.0) {
				addToFlowQueue(veh, now);
				this.storageQueue.poll();
				break;
			} else {
				break;
			}
		}
	}

	private void updateBufferCapacity() {
		if (this.flowCapacityFractionalRest < 1.0) {
			this.flowCapacityFractionalRest += this.flowCapacityFraction;
		}
	}

	private boolean hasFlowQueueSpace() {
		return ((this.flowQueue.size() < this.flowCapacityCeil) && ((this.flowCapacity >= 1.0) || (this.flowCapacityFractionalRest >= 1.0)));
	}

	private void addToFlowQueue(final Vehicle veh, final double now) {

		if (this.maximumFlowCapacity >= 1.0) {
			this.maximumFlowCapacity--;
		} else if (this.flowCapacityFractionalRest >= 1.0) {
			this.flowCapacityFractionalRest--;
		} else {
			// throw new RuntimeException("Buffer of link " + this.link.getId() + " has no space left!");
		}

		this.flowQueue.add(veh);
		veh.setLastMovedTime(now);
	}

	// Decision to only consider the StorageQueue when calculating hasSpace
	// Vehicles stored in the FlowQueue are ignored
	public boolean hasSpace() {
		if (this.storageQueue.size() < this.storageCapacity) {
			return true;
		}
		return false;
	}

	private void moveFlowQueueToNextPseudoLink() {

		boolean moveOn = true;

		while (moveOn && !this.flowQueue.isEmpty() && (this.toLinks.size() != 0)) {
			Vehicle veh = this.flowQueue.peek();
			Link nextLink = veh.getDriver().chooseNextLink();

			if (nextLink != null) {
				for (PseudoLink toLink : this.toLinks) {
					for (Link qLink : toLink.getDestLinks()) {
						if (qLink.equals(nextLink)) {
							if (toLink.hasSpace()) {
								this.flowQueue.poll();
								toLink.addVehicle(veh);
							} else
								moveOn = false;
						}
					}
				}
			}
		}
	}

	public List<Link> getDestLinks() {
		return this.destLinks;
	}

	public void addDestLink(Link destLink) {
		this.destLinks.add(destLink);
	}

	public void addVehicle(Vehicle vehicle) {
		this.storageQueue.add(vehicle);
		if (this.amIOriginalLink) {
			// It's the original link,
			// so we need to start with a 'clean' freeSpeedTravelTime
			vehicle.setDepartureTime_s(SimulationTimer.getTime() + this.freeSpeedTravelTime);
		} else {
			// It's not the original link,
			// so there is a fractional rest we add to this link's freeSpeedTravelTime
			vehicle.setDepartureTime_s(SimulationTimer.getTime() + this.freeSpeedTravelTime
					+ vehicle.getDepartureTime_s() - Math.floor(vehicle.getDepartureTime_s()));
		}

		if (this.meterFromLinkEnd == 0) {
			// It's a nodePseudoLink,
			// so we have to floor the freeLinkTravelTime in order the get the same
			// results compared to the old mobSim
			vehicle.setDepartureTime_s(Math.floor(vehicle.getDepartureTime_s()));
		}
	}

	private void moveParkingQueueToParkToLinkQueue(final double now) {
		Vehicle veh;
		while ((veh = this.parkingQueue.peek()) != null) {
			if (veh.getDepartureTime_s() > now) {
				break;
			}
			veh.getDriver().leaveActivity(now);
			QueueSimulation.getEvents().processEvent(new AgentDepartureEvent(now, veh.getDriver().getPerson(),
							this.realLink.getLink(), veh.getCurrentLeg()));
			Leg actLeg = veh.getCurrentLeg();
			if (actLeg.getRoute().getRoute().size() != 0) {
				this.parkToLinkQueue.add(veh);
			}
			this.parkingQueue.poll();
		}
	}

	/**
	 * Move as many waiting cars to the link as it is possible
	 * 
	 * @param now the current time
	 */
	private void moveParkToLinkQueueToFlowQueue(final double now) {
		Vehicle veh;
		while ((veh = this.parkToLinkQueue.peek()) != null) {
			if (!hasFlowQueueSpace()) {
				break;
			}
			addToFlowQueue(veh, now);
			QueueSimulation.getEvents().processEvent(new AgentWait2LinkEvent(now, veh.getDriver().getPerson(),
							this.realLink.getLink(), veh.getCurrentLeg()));
			 // remove the just handled vehicle from parkToLinkQueue
			this.parkToLinkQueue.poll();
		}
	}

	public void addVehicle2ParkingQueue(Vehicle veh) {
		this.parkingQueue.add(veh);
	}

	public double getMeterFromLinkEnd() {
		return this.meterFromLinkEnd;
	}

	public List<PseudoLink> getToLinks() {
		return this.toLinks;
	}

	boolean flowQueueIsEmpty() {
		return this.flowQueue.isEmpty();
	}

	Vehicle getFirstFromBuffer() {
		return this.flowQueue.peek();
	}

	Vehicle pollFirstFromBuffer() {
		double now = SimulationTimer.getTime();
		Vehicle veh = this.flowQueue.poll();

		QueueSimulation.getEvents().processEvent(
				new LinkLeaveEvent(now, veh.getDriver().getPerson(), this.realLink.getLink(), veh.getCurrentLeg()));

		return veh;
	}

	
	// --- Methods for Visualizers ---
	
	double getMaxPossibleNumberOfVehOnLink() {
		return this.storageCapacity + this.flowCapacityCeil;
	}

	void getVehPositions(final Collection<PositionInfo> positions) {

		double now = SimulationTimer.getTime();
		int cnt = 0;

		// the position of the start of the queue jammed vehicles build at the end of the link
		double queueEnd = this.realLink.getLink().getLength() - this.meterFromLinkEnd;

		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		// the length of a vehicle in visualization, all vehicles must have place on the link,
		// a vehicle should not be larger than it's actual size
		double vehLen = Math.min(this.length_m / (this.getMaxPossibleNumberOfVehOnLink()),
				((NetworkLayer) this.realLink.getLink().getLayer()).getEffectiveCellSize() / storageCapFactor);

		// put all cars in the buffer one after the other
		for (Vehicle veh : this.flowQueue) {

			int lane = this.visualizerLane;

			int cmp = (int) (veh.getDepartureTime_s() + (1.0 / this.realLink.getSimulatedFlowCapacity()) + 2.0);
			double speed = (now > cmp) ? 0.0 : this.realLink.getLink().getFreespeed(Time.UNDEFINED_TIME);

			PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), this.realLink.getLink(),
					queueEnd, lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getPerson().getVisualizerData());
			positions.add(position);
			cnt++;
			queueEnd -= vehLen;
		}

		/*
		 * place other driving cars according the following rule: - calculate
		 * the time how long the vehicle is on the link already - calculate the
		 * position where the vehicle should be if it could drive with freespeed -
		 * if the position is already within the congestion queue, add it to the
		 * queue with slow speed - if the position is not within the queue, just
		 * place the car with free speed at that place
		 */
		double lastDistance = Integer.MAX_VALUE;
		for (Vehicle veh : this.storageQueue) {
			double travelTime = now - (veh.getDepartureTime_s() - this.realLink.getLink().getFreespeedTravelTime(now));
			double distanceOnLink = (this.realLink.getLink().getFreespeedTravelTime(now) == 0.0 ? 0.0
					: ((travelTime / this.realLink.getLink().getFreespeedTravelTime(now)) * (this.realLink.getLink().getLength() - this.meterFromLinkEnd)));
			if (distanceOnLink > queueEnd) { // vehicle is already in queue
				distanceOnLink = queueEnd;
				queueEnd -= vehLen;
			}
			if (distanceOnLink >= lastDistance) {
				/*
				 * we have a queue, so it should not be possible that one
				 * vehicles overtakes another. additionally, if two vehicles
				 * entered at the same time, they would be drawn on top of each
				 * other. we don't allow this, so in this case we put one after
				 * the other. Theoretically, this could lead to vehicles placed
				 * at negative distance when a lot of vehicles all enter at the
				 * same time on an empty link. not sure what to do about this
				 * yet... just setting them to 0 currently.
				 */
				distanceOnLink = lastDistance - vehLen;
				if (distanceOnLink < 0)
					distanceOnLink = 0.0;
			}
			int cmp = (int) (veh.getDepartureTime_s() + (1.0 / this.realLink.getSimulatedFlowCapacity()) + 2.0);
			double speed = (now > cmp) ? 0.0 : this.realLink.getLink().getFreespeed(now);
			int lane = this.visualizerLane;
			PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), this.realLink.getLink(),
					distanceOnLink, lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getPerson().getVisualizerData());
			positions.add(position);
			lastDistance = distanceOnLink;
		}

		int lane = this.visualizerLane; // place them next to the link

		/*
		 * Put the vehicles from the waiting list in positions. Their actual
		 * position doesn't matter, so they are just placed to the coordinates
		 * of the from node
		 */

		for (Vehicle veh : this.parkToLinkQueue) {
			PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), this.realLink.getLink(),
					((NetworkLayer) this.realLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
					PositionInfo.VehicleState.Parking, veh.getDriver().getPerson().getVisualizerData());
			positions.add(position);
		}

		/*
		 * put the vehicles from the parking list in positions their actual
		 * position doesn't matter, so they are just placed to the coordinates
		 * of the from node
		 */
		lane = this.visualizerLane; // place them next to the link
		for (Vehicle veh : this.parkingQueue) {
			PositionInfo position = new PositionInfo(veh.getDriver().getPerson().getId(), this.realLink.getLink(),
					((NetworkLayer) this.realLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
					PositionInfo.VehicleState.Parking, veh.getDriver().getPerson().getVisualizerData());
			positions.add(position);
		}

	}

	// --- Implementation of Comparable interface ---
	// Sorts SubLinks of a QueueLink 
	
	public int compareTo(PseudoLink otherPseudoLink) {
		if (this.meterFromLinkEnd < otherPseudoLink.meterFromLinkEnd) {
			return -1;
		} else if (this.meterFromLinkEnd > otherPseudoLink.meterFromLinkEnd) {
			return 1;
		} else {
			return 0;
		}
	}

}