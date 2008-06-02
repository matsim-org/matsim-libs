package playground.andreas.intersection.sim;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkLeave;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.mobsim.Vehicle;
import org.matsim.mobsim.VehicleDepartureTimeComparator;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

public class PseudoLink {

	/** Logger */
	@SuppressWarnings("unused")
	final private static Logger log = Logger.getLogger(QLink.class);

	/** Id of the real link, null if this is not the first/original <code>PseudoLink</code> */
	private QLink realLink = null;
	private boolean amIOriginalLink = false;

	/** Hält alle echten Ziellinks, die von diesem Link erreichbar sind, meist 1-3 */
	private List<Link> destLinks = new LinkedList<Link>();

	/** Next PseudoLink upstream, null if first link */
	private PseudoLink fromLink = null;

	/** Contains next PseudoLinks downstream, null if last link */
	private List<PseudoLink> toLinks = new LinkedList<PseudoLink>();

	/** Meter counted from the end of the real link */
	private double meterFromLinkEnd = -1;

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
			new VehicleDepartureTimeComparator());

	/** All vehicles from parkingQueue move to the waitingList as soon as their time
	 * has come. They are then filled into the storageQueue, depending on free space
	 * in the storageQueue */
	private final Queue<QVehicle> parkToLinkQueue = new LinkedList<QVehicle>();

	// Helper
	private double flowCapacityCeil = Double.NaN;
	private double flowCapacityFraction = Double.NaN;

	private double flowCapacityFractionalRest = 1.0;
	
	/** For Visualization only */
	int lane = 1;
	double length_m = -1;


	public PseudoLink(QLink originalLink, boolean amIOriginalLink) {
		this.realLink = originalLink;
		this.amIOriginalLink = amIOriginalLink;
	}

	public boolean recalculatePseudoLinkProperties(double meterFromLinkEnd_m, double length_m, int numberOfLanes, double freeSpeed_m_s, double flowCapacityFromNetFile_Veh_h, double effectiveCellSize){

		this.length_m = length_m;
		this.meterFromLinkEnd = meterFromLinkEnd_m;

		this.freeSpeedTravelTime = length_m / freeSpeed_m_s;

		this.flowCapacity = flowCapacityFromNetFile_Veh_h * SimulationTimer.getSimTickTime() * Gbl.getConfig().simulation().getFlowCapFactor();

		this.flowCapacityCeil = (int) Math.ceil(this.flowCapacity);
		this.flowCapacityFraction = this.flowCapacity - (int) this.flowCapacity;

		this.storageCapacity = (length_m * numberOfLanes) / effectiveCellSize * Gbl.getConfig().simulation().getStorageCapFactor();

		this.storageCapacity = Math.max(this.storageCapacity, this.flowCapacityCeil);

		if (this.storageCapacity < this.freeSpeedTravelTime * this.flowCapacity) {
			this.storageCapacity = this.freeSpeedTravelTime * this.flowCapacity;
			return false;

		} else return true;

	}

	public void movePseudoLink(final double now){

		if (this.amIOriginalLink){ moveParkingQueueToParkToLinkQueue(now); }
		moveFlowQueueToNextPseudoLink();
		moveStorageQueueToFlowQueue(now);
		if (this.amIOriginalLink){ moveParkToLinkQueueToFlowQueue(now); }

	}

	private void moveStorageQueueToFlowQueue(final double now) {

		double maximumFlowCapacity = this.flowCapacity;

		QVehicle veh;
		while ((veh = (QVehicle)this.storageQueue.peek()) != null) {
			if (veh.getDepartureTime_s() > now) {
				break;
			}

			if (veh.getDestinationLink().getId() == this.realLink.getLink().getId()) {

				QSim.getEvents().processEvent(new EventAgentArrival(now, veh.getDriver().getId().toString(), veh.getCurrentLegNumber(),
						this.realLink.getLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink.getLink()));
				veh.reachActivity(now, this.realLink);
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
		
//		if(!this.hasFlowQueueSpace() && this.amIOriginalLink == false && meterFromLinkEnd == 0 && this.flowCapacityFractionalRest > 1.0){
//			this.flowCapacityFractionalRest = 1.0;
//		}

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

		Boolean moveOn = true;

		while(moveOn && !this.flowQueue.isEmpty() && (this.toLinks.size() != 0)){
			QVehicle veh = this.flowQueue.peek();
			Link nextLink = (Link) veh.chooseNextLink();

			if (nextLink != null) {
				for (PseudoLink toLink : this.toLinks) {
					for (Link qLink : toLink.getDestLinks()) {
						if (qLink.equals(nextLink)){
							if (toLink.hasSpace()) {
								this.flowQueue.poll();
								toLink.addVehicle(veh);
							}else moveOn = false;
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

			veh.leaveActivity(now);

			QSim.getEvents().processEvent(new EventAgentDeparture(now, veh.getDriver().getId().toString(), veh.getCurrentLegNumber(),
					this.realLink.getLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink.getLink()));

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

			QSim.getEvents().processEvent(new EventAgentWait2Link(now, veh.getDriver().getId().toString(), veh.getCurrentLegNumber(), this.realLink.getLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink.getLink()));

			this.parkToLinkQueue.poll(); // remove the just handled vehicle from parkToLinkQueue
		}
	}

	public void addVehicle2ParkingQueue(QVehicle veh) {
		this.parkingQueue.add(veh);
	}

	public Queue<QVehicle> getFlowQueue(){
		return this.flowQueue;
	}
	
	public Queue<QVehicle> getStorageQueue(){
		return this.storageQueue;
	}
	
	public Queue<QVehicle> getParkToLinkQueue(){
		return parkToLinkQueue;
	}
	
	public Queue<QVehicle> getParkingQueue(){
		return this.parkingQueue;
	}

	public double getMeterFromLinkEnd() {
		return this.meterFromLinkEnd;
	}

	public PseudoLink getFromLink() {
		return this.fromLink;
	}

	public void setFromLink(PseudoLink fromLink) {
		this.fromLink = fromLink;
	}

	public List<PseudoLink> getToLinks() {
		return this.toLinks;
	}

	public void setToLinks(List<PseudoLink> toLinks) {
		this.toLinks = toLinks;
	}

	boolean flowQueueIsEmpty() {
		return this.flowQueue.isEmpty();
	}

	QVehicle getFirstFromBuffer() {
		
//		double maximumFlowCapacity = this.flowCapacity;
//
//		QVehicle veh;
//		while ((veh = (QVehicle)this.storageQueue.peek()) != null) {
//			if (veh.getDepartureTime_s() > now) {
//				break;
//			}
//
//			if (veh.getDestinationLink().getId() == this.realLink.getLink().getId()) {
//
//				QSim.getEvents().processEvent(new EventAgentArrival(now, veh.getDriver().getId().toString(), veh.getCurrentLegNumber(),
//						this.realLink.getLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this.realLink.getLink()));
//				veh.reachActivity(now, this.realLink);
//				this.storageQueue.poll();
//				continue;
//			}
//
//			if (!hasFlowQueueSpace()) {
//				break;
//			}
//
//			if (maximumFlowCapacity >= 1.0) {
//				maximumFlowCapacity--;
//				addToFlowQueue(veh, now);
//				this.storageQueue.poll();
//				continue;
//
//			} else if (this.flowCapacityFractionalRest >= 1.0) {
//				this.flowCapacityFractionalRest--;
//				addToFlowQueue(veh, now);
//				this.storageQueue.poll();
//				break;
//			} else {
//				break;
//			}
//		}
//
//		if (this.flowCapacityFractionalRest < 1.0) {
//			this.flowCapacityFractionalRest += this.flowCapacityFraction;
//		}
		
		
		
		
		return this.flowQueue.peek();
	}

	QVehicle pollFirstFromBuffer() {
		double now = SimulationTimer.getTime();
		QVehicle veh = this.flowQueue.poll();

		QSim.getEvents().processEvent(new EventLinkLeave(now, veh.getDriver().getId().toString(), veh.getCurrentLegNumber(),
						this.realLink.getLink().getId().toString(), veh.getDriver(), this.realLink.getLink()));

		return veh;
	}
	
	double getMaxPossibleNumberOfVehOnLink(){
		return this.storageCapacity + this.flowCapacityCeil;
	}

	void getVehPositions(final Collection<PositionInfo> positions){

		double now = SimulationTimer.getTime();
		int cnt = 0;

		double queueEnd = this.realLink.getLink().getLength() - meterFromLinkEnd; // the position of the start of the queue jammed vehicles build at the end of the link

		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		double vehLen = Math.min( // the length of a vehicle in visualization
				this.length_m / (this.getMaxPossibleNumberOfVehOnLink()), // all vehicles must have place on the link
				((NetworkLayer)this.realLink.getLink().getLayer()).getEffectiveCellSize() / storageCapFactor); // a vehicle should not be larger than it's actual size


		// put all cars in the buffer one after the other
		for (Vehicle veh : this.flowQueue) {

			int lane = this.lane;

			int cmp = (int) (veh.getDepartureTime_s() + (1.0 / this.realLink.getSimulatedFlowCapacity()) + 2.0);
			double speed = (now > cmp) ? 0.0 : this.realLink.getLink().getFreespeed(Time.UNDEFINED_TIME);
			veh.setSpeed(speed);

			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.realLink.getLink(), queueEnd,
					lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getVisualizerData());
			positions.add(position);
			cnt++;
			queueEnd -= vehLen;
		}

		/*
		 * place other driving cars according the following rule:
		 * - calculate the time how long the vehicle is on the link already
		 * - calculate the position where the vehicle should be if it could drive with freespeed
		 * - if the position is already within the congestion queue, add it to the queue with slow speed
		 * - if the position is not within the queue, just place the car 	with free speed at that place
		 */
		double lastDistance = Integer.MAX_VALUE;
		for (Vehicle veh : this.storageQueue) {
			double travelTime = now - (veh.getDepartureTime_s() - this.realLink.getLink().getFreespeedTravelTime(now));
			double distanceOnLink = (this.realLink.getLink().getFreespeedTravelTime(now) == 0.0 ? 0.0
					: ((travelTime / this.realLink.getLink().getFreespeedTravelTime(now)) * (this.realLink.getLink().getLength() - meterFromLinkEnd)));
			if (distanceOnLink > queueEnd) { // vehicle is already in queue
				distanceOnLink = queueEnd;
				queueEnd -= vehLen;
			}
			if (distanceOnLink >= lastDistance) {
				/*
				 * we have a queue, so it should not be possible that one vehicles
				 * overtakes another. additionally, if two vehicles entered at the same
				 * time, they would be drawn on top of each other. we don't allow this,
				 * so in this case we put one after the other. Theoretically, this could
				 * lead to vehicles placed at negative distance when a lot of vehicles
				 * all enter at the same time on an empty link. not sure what to do
				 * about this yet... just setting them to 0 currently.
				 */
				distanceOnLink = lastDistance - vehLen;
				if (distanceOnLink < 0)
					distanceOnLink = 0.0;
			}
			int cmp = (int) (veh.getDepartureTime_s()
					+ (1.0 / this.realLink.getSimulatedFlowCapacity()) + 2.0);
			double speed = (now > cmp) ? 0.0 : this.realLink.getLink().getFreespeed(now);
			veh.setSpeed(speed);
			int lane = this.lane;
			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.realLink.getLink(), distanceOnLink,
					lane, speed, PositionInfo.VehicleState.Driving, veh.getDriver().getVisualizerData());
			positions.add(position);
			lastDistance = distanceOnLink;
		}

		int lane = this.lane; // place them next to the link

		/*
		 * Put the vehicles from the waiting list in positions. Their actual
		 * position doesn't matter, so they are just placed to the coordinates of
		 * the from node
		 */

		for (Vehicle veh : this.parkToLinkQueue) {
			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.realLink.getLink(),
					((NetworkLayer) this.realLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
					PositionInfo.VehicleState.Parking, veh.getDriver().getVisualizerData());
			positions.add(position);
		}

		/*
		 * put the vehicles from the parking list in positions their actual position
		 * doesn't matter, so they are just placed to the coordinates of the from
		 * node
		 */
		lane = this.lane; // place them next to the link
		for (Vehicle veh : this.parkingQueue) {
			PositionInfo position = new PositionInfo(veh.getDriver().getId(), this.realLink.getLink(),
					((NetworkLayer) this.realLink.getLink().getLayer()).getEffectiveCellSize(), lane, 0.0,
					PositionInfo.VehicleState.Parking, veh.getDriver().getVisualizerData());
			positions.add(position);
		}			

	}

}
