/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;

import java.util.*;

/**
 * Separating out the "lane" functionality from the "link" functionality also for QLinkImpl.  Ultimate goal is to unite this class here
 * with QLane.
 * <p/>
 * Design thoughts:<ul>
 * <li> It seems a bit doubtful why something this data structure needs to know something like "hasGreenForToLink(Id)".
 * The alternative, I guess, would be to have this in the surrounding QLink(Lanes)Impl.  Since the info is different for each lane,
 * after thinking about it it makes some sense to attach this directly to the lanes.  kai, jun'13
 * <li> A design problem with this class is that it pulls its knowledge (such as length, capacity,
 * ...) from the link, rather than getting it set explicitly.  As a result, one needs to replace
 * "pulling from the link" by "pulling from the laneData" for lanes. :-(  kai, sep'13
 * </ul>
 *
 * @author nagel
 */
final class QueueWithBuffer extends QLaneI implements SignalizeableItem {
	private static final Logger log = Logger.getLogger( QueueWithBuffer.class ) ;

	/**
	 * The remaining integer part of the flow capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateRemainingFlowCapacity()} (double)}.
	 */
	private double remainingflowCap = 0.0 ;
	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 * <p/>
	 * I changed this into an internal class as a first step to look into acceleration (not having to keep this link active until
	 * this has accumulated to one).  There is no need to keep it this way; it just seems to make it easier to keep track of
	 * changes.  kai, sep'14
	 */
	class FlowcapAccumulate {
		private double timeStep = 0.;//Double.NEGATIVE_INFINITY ;
		private double value = 0. ;
		double getTimeStep() {
			return timeStep;
		}
		double getValue() {
			return value;
		}
		void setValue(double value ) {
			this.value = value;
		}
		void addValue(double value1, double now) {
			this.value += value1;
			this.timeStep = now ;
		}
	}
	private final FlowcapAccumulate flowcap_accumulate = new FlowcapAccumulate() ;
	// might be changed back to standard double after all of this was figured out. kai, sep'14

	/**
	 * true, i.e. green, if the link is not signalized
	 */
	private boolean thisTimeStepGreen = true ;
	private double inverseFlowCapacityPerTimeStep;
	private double flowCapacityPerTimeStepFractionalPart;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep;
	private int bufferStorageCapacity;
	private double usedBufferStorageCapacity = 0.0 ;
	private double remainingHolesStorageCapacity = 0.0 ;

	private final Queue<QueueWithBuffer.Hole> holes = new LinkedList<QueueWithBuffer.Hole>();

	private double freespeedTravelTime = Double.NaN;
	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.UNDEFINED_TIME ;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final VehicleQ<QVehicle> vehQueue;

	private double storageCapacity;
	double usedStorageCapacity;
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<QVehicle> buffer = new LinkedList<>() ;
	/**
	 * null if the link is not signalized
	 */
	private DefaultSignalizeableItem qSignalizedItem = null ;
	private final AbstractQLink qLink;
	private final Link link ; // I want to know where we really need the qLink.  kai, sep'14
	private final QNetwork network ;
	private final Id<Lane> id;
	private static int spaceCapWarningCount = 0;
	static boolean HOLES = false ; // can be set from elsewhere in package, but not from outside.  kai, nov'10
	static boolean VIS_HOLES = false ;
	private double hole_speed = 15.0;
	/**
	 * LaneEvents should only be fired if there is more than one QueueLane on a QueueLink
	 * because the LaneEvents are identical with LinkEnter/LeaveEvents otherwise.
	 * Set to "true" in QLinkImpl.
	 */
	boolean generatingEvents = false;

	// get properties no longer from qlink, but have them by yourself:
	// NOTE: we need to have qlink since we need access e.g. for vehicle arrival or for public transit
	// On the other hand, the qlink properties (e.g. number of lanes) may not be the ones we need here because they
	// may be divided between parallel lanes.  So we need both.
	private double length = Double.NaN ;
	private double unscaledFlowCapacity_s = Double.NaN ;
	private double effectiveNumberOfLanes = Double.NaN ;

	// (still) private:
	private final VisData visData = new VisDataImpl() ;
	private final double timeStepSize;

	private boolean fastCapacityUpdate;

	static class Builder {
		private VehicleQ<QVehicle> vehicleQueue = null ;
		private Id<Lane> id = null ;
		private Double length = null ;
		private Double effectiveNumberOfLanes = null ;
		private Double flowCapacity_s = null ;
		private AbstractQLink qLink;
		/**
		 * @param qLink -- The embedding qLink is needed, for example to park vehicles or to activate toNodes.
		 */
		Builder( AbstractQLink qLink ) {
			this.qLink = qLink ;
		}
		QueueWithBuffer build() {
			if ( vehicleQueue == null ) {
				vehicleQueue = new FIFOVehicleQ() ;
			}
			if ( id==null ) {
				id = Id.create( qLink.getLink().getId() , Lane.class ) ;
			}
			if ( length==null ) {
				length = qLink.getLink().getLength() ;
			}
			if ( effectiveNumberOfLanes==null ) {
				effectiveNumberOfLanes = qLink.getLink().getNumberOfLanes() ;
			}
			if ( flowCapacity_s==null ) {
				flowCapacity_s = ((LinkImpl)qLink.getLink()).getFlowCapacity() ;
			}
			return new QueueWithBuffer( qLink, vehicleQueue, id, length, effectiveNumberOfLanes, flowCapacity_s ) ;
		}
		/**
		 * @param vehicleQueue -- may be set away from its default.
		 */
		void setVehicleQueue(VehicleQ<QVehicle> vehicleQueue) {
			this.vehicleQueue = vehicleQueue;
		}
		/**
		 * @param id -- may be different from the QLink's ID (e.g. for lanes)
		 */
		void setId(Id<Lane> id) {
			this.id = id;
		}
		/**
		 * @param length -- may be different from the QLink's lane (e.g. for lanes)
		 */
		void setLength(Double length) {
			this.length = length;
		}
		/**
		 * @param effectiveNumberOfLanes -- may be different from the QLink's lane (e.g. for lanes)
		 */
		void setEffectiveNumberOfLanes(Double effectiveNumberOfLanes) {
			this.effectiveNumberOfLanes = effectiveNumberOfLanes;
		}
		/**
		 * @param flowCapacity_s -- may be different from the QLink's lane (e.g. for lanes)
		 * 
		 * Probably not useful since not a constant inside QLane!
		 */
		void setFlowCapacity_s(Double flowCapacity_s) {
			this.flowCapacity_s = flowCapacity_s;
		}
	}

	private QueueWithBuffer(AbstractQLink qLinkImpl,  final VehicleQ<QVehicle> vehicleQueue, Id<Lane> id, 
			double length, double effectiveNumberOfLanes, double flowCapacity_s) {
		this.id = id ;
		this.qLink = qLinkImpl;
		this.link = qLinkImpl.link ;
		this.network = qLinkImpl.network ;
		this.vehQueue = vehicleQueue ;

		this.length = length;
		this.unscaledFlowCapacity_s = flowCapacity_s ;
		this.effectiveNumberOfLanes = effectiveNumberOfLanes;

		final QSimConfigGroup qsimConfig = this.network.simEngine.getMobsim().getScenario().getConfig().qsim();

		this.timeStepSize = qsimConfig.getTimeStepSize() ;
		this.fastCapacityUpdate = qsimConfig.isUsingFastCapacityUpdate() ;
		
		String isSeeping = this.network.simEngine.getMobsim().getScenario().getConfig().findParam("seepage", "isSeepageAllowed");

		if(isSeeping != null && isSeeping.equals("true")){
			this.seepageAllowed = true;
			this.seepMode = this.network.simEngine.getMobsim().getScenario().getConfig().getParam("seepage", "seepMode");
			this.isSeepModeStorageFree = Boolean.valueOf(this.network.simEngine.getMobsim().getScenario().getConfig().getParam("seepage", "isSeepModeStorageFree"));
			log.info("Seepage is allowed. Seep mode is "+this.seepMode+".");
			if(this.isSeepModeStorageFree) log.warn("Seep mode "+seepMode+" do not take storage space thus only considered for flow capacities.");
		}

		//following are copied part from QNetSimEngine instead of copying the whole class.
		QSimConfigGroup qsimConfigGroup = qsimConfig;
		if ( QSimConfigGroup.TrafficDynamics.queue.equals( qsimConfigGroup.getTrafficDynamics() ) ) {
			QueueWithBuffer.HOLES=false ;
		} else if ( QSimConfigGroup.TrafficDynamics.withHoles.equals( qsimConfigGroup.getTrafficDynamics() ) ) {
			QueueWithBuffer.HOLES = true ;
		} else {
			throw new RuntimeException("trafficDynamics defined in config that does not exist: "
					+ qsimConfigGroup.getTrafficDynamics() ) ;
		}
		if ( QSimConfigGroup.SnapshotStyle.withHoles.equals( qsimConfigGroup.getSnapshotStyle() ) ) {
			QueueWithBuffer.VIS_HOLES = true ;
		}

		if(QueueWithBuffer.HOLES && this.network.simEngine.getMobsim().getScenario().getConfig().getModule("WITH_HOLE")!=null){
			hole_speed = Double.valueOf(this.network.simEngine.getMobsim().getScenario().getConfig().getParam("WITH_HOLE", "HOLE_SPEED"));
			if (hole_speed!=15) {
				log.warn("Hole speed is set to "+hole_speed+". Default hardcoded value is 15.");
			}
		}

		freespeedTravelTime = this.length / qLinkImpl.getLink().getFreespeed();
		if (Double.isNaN(freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}
		this.calculateFlowCapacity();
		this.calculateStorageCapacity();

		if(this.fastCapacityUpdate){
			flowcap_accumulate.setValue(flowCapacityPerTimeStep);
		} else {
			flowcap_accumulate.setValue((flowCapacityPerTimeStepFractionalPart == 0.0 ? 0.0 : 1.0) );
		}
		
		if ( this.network.simEngine.getMobsim().getSimTimer().getSimTimestepSize()<1.) {
			throw new RuntimeException("yyyy This will produce weird results because in at least one place "
					+ "(addFromUpstream(...)) everything is pulled to integer values.  Aborting ... "
					+ "(This statement may no longer be correct; I think that the incriminating code was modified.  So please test and remove"
					+ " the warning if it works. kai, sep'14") ;
		}
	}

	@Override
	public final void addFromWait(final QVehicle veh, final double now) {
		addToBuffer(veh, now);
	}

	private void addToBuffer(final QVehicle veh, final double now) {
		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12
		
		if(this.fastCapacityUpdate){
			updateFlowAccumulation(now);
			if (flowcap_accumulate.getValue() > 0.0  ) {
				flowcap_accumulate.addValue(-veh.getSizeInEquivalents(), now);
			}
			else {
				throw new IllegalStateException("Buffer of link " + this.id + " has no space left!");
			}
		} else {
			if (remainingflowCap >= 1.0  ) {
				remainingflowCap -= veh.getSizeInEquivalents();
			}
			else if (flowcap_accumulate.getValue() >= 1.0) {
				flowcap_accumulate.setValue(flowcap_accumulate.getValue() - veh.getSizeInEquivalents() );
			}
			else {
				throw new IllegalStateException("Buffer of link " + this.id + " has no space left!");
			}
		}

		
		buffer.add(veh);
		usedBufferStorageCapacity = usedBufferStorageCapacity + veh.getSizeInEquivalents();
		if (buffer.size() == 1) {
			bufferLastMovedTime = now;
			// (if there is one vehicle in the buffer now, there were zero vehicles in the buffer before.  in consequence,
			// need to reset the lastMovedTime.  If, in contrast, there was already a vehicle in the buffer before, we can
			// use the lastMovedTime that was (somehow) computed for that vehicle.)
		}
		qLink.getToNode().activateNode();
		// yy for an "upstream" QLane, this activates the toNode too early.  Yet I think I founds this
		// also in the original QLane code.  kai, sep'13
	}

	@Override
	public final boolean isAcceptingFromWait() {
		return this.hasFlowCapacityLeftAndBufferSpace() ;
	}

	private boolean hasFlowCapacityLeftAndBufferSpace() {
		final double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;

		if(this.fastCapacityUpdate){
			updateFlowAccumulation(now);
			return (
					usedBufferStorageCapacity < bufferStorageCapacity
					&&
					((flowcap_accumulate.getValue() > 0.0) )
					);
		} else {
			return (
					usedBufferStorageCapacity < bufferStorageCapacity
					&&
					((remainingflowCap >= 1.0) || this.flowcap_accumulate.getValue() >=1.0)
					);
		}
	}

	private void updateFlowAccumulation(final double now){

		if( this.flowcap_accumulate.getTimeStep() < now && this.flowcap_accumulate.getValue() <= 0 && isNotOfferingVehicle() ){

			double flowCapSoFar = flowcap_accumulate.getValue();
			double newStoredFlowCap = (now - flowcap_accumulate.getTimeStep()) * flowCapacityPerTimeStep;
			double totalFlowCap = flowCapSoFar + newStoredFlowCap;

			if(totalFlowCap > flowCapacityPerTimeStep) {
				flowcap_accumulate.setValue(flowCapacityPerTimeStep);
				flowcap_accumulate.timeStep = now;
			}else {
				flowcap_accumulate.addValue(newStoredFlowCap,now);
			}
		}
	}

	@Override
	public final void updateRemainingFlowCapacity() {
		if(!this.fastCapacityUpdate){
			remainingflowCap = flowCapacityPerTimeStep;
			if (thisTimeStepGreen && flowcap_accumulate.getValue() < 1.0 && isNotOfferingVehicle() ) {
				final double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;
				flowcap_accumulate.addValue( flowCapacityPerTimeStepFractionalPart, now);
			}
		}
	}

	private void calculateFlowCapacity() {
		flowCapacityPerTimeStep = this.unscaledFlowCapacity_s ;
		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		flowCapacityPerTimeStep = flowCapacityPerTimeStep
				* network.simEngine.getMobsim().getSimTimer().getSimTimestepSize()
				* network.simEngine.getMobsim().getScenario().getConfig().qsim().getFlowCapFactor();
		inverseFlowCapacityPerTimeStep = 1.0 / flowCapacityPerTimeStep;
		flowCapacityPerTimeStepFractionalPart = flowCapacityPerTimeStep - (int) flowCapacityPerTimeStep;
	}

	private void calculateStorageCapacity() {
		double storageCapFactor = network.simEngine.getMobsim().getScenario().getConfig().qsim().getStorageCapFactor();
		bufferStorageCapacity = (int) Math.ceil(flowCapacityPerTimeStep);

		// first guess at storageCapacity:
		storageCapacity = (this.length * this.effectiveNumberOfLanes)
				/ ((NetworkImpl) network.simEngine.getMobsim().getScenario().getNetwork()).getEffectiveCellSize() * storageCapFactor;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		storageCapacity = Math.max(storageCapacity, bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap = TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = freespeedTravelTime * flowCapacityPerTimeStep;
		// yy note: freespeedTravelTime may be Inf.  In this case, storageCapacity will also be set to Inf.  This can still be
		// interpreted, but it means that the link will act as an infinite sink.  kai, nov'10

		if (storageCapacity < tempStorageCapacity) {
			if (QueueWithBuffer.spaceCapWarningCount <= 10) {
				log.warn("Link " + this.id + " too small: enlarge storage capacity from: " + storageCapacity
						+ " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (QueueWithBuffer.spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				QueueWithBuffer.spaceCapWarningCount++;
			}
			storageCapacity = tempStorageCapacity;
		}

		if ( QueueWithBuffer.HOLES ) {
			remainingHolesStorageCapacity = this.storageCapacity;
		}
	}

	@Override
	public boolean doSimStep(final double now ) {
		if(QueueWithBuffer.HOLES) this.processArrivalOfHoles( now ) ;
		this.moveQueueToBuffer(now);
		return true ;
	}

	private void processArrivalOfHoles(double now) {
		while ( this.holes.size()>0 && this.holes.peek().getEarliestLinkExitTime() < now ) {
			Hole hole = this.holes.poll() ; // ???
			this.remainingHolesStorageCapacity += hole.getSizeInEquivalents() ;
		}
	}
	

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 * @param now
	 *          The current time.
	 */
	final void moveQueueToBuffer(final double now) {
		QVehicle veh;

		//		while ((veh = vehQueue.peek()) != null) {
		while((veh = peekFromVehQueue()) !=null){
			//we have an original QueueLink behaviour
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}

			MobsimDriverAgent driver = veh.getDriver();

			if (driver instanceof TransitDriverAgent) {
				HandleTransitStopResult handleTransitStop = qLink.transitQLink.handleTransitStop(now, veh, (TransitDriverAgent) driver, this.qLink.link.getId());
				if (handleTransitStop == HandleTransitStopResult.accepted) {
					// vehicle has been accepted into the transit vehicle queue of the link.
					removeVehicleFromQueue(now,veh) ;
					continue;
				} else if (handleTransitStop == HandleTransitStopResult.rehandle) {
					continue; // yy why "continue", and not "break" or "return"?  Seems to me that this
					// is currently only working because qLink.handleTransitStop(...) also increases the
					// earliestLinkExitTime for the present vehicle.  kai, oct'13
					// zz From my point of view it is exactly like described above. dg, mar'14
				} else if (handleTransitStop == HandleTransitStopResult.continue_driving) {
					// Do nothing, but go on..
				}
			}

			// Check if veh has reached destination:
//			if ((driver.chooseNextLinkId() == null)) {
			if ((driver.isWantingToArriveOnCurrentLink())) {
				letVehicleArrive(now, veh);
				continue;
			}

			/* is there still room left in the buffer? */
			if (!hasFlowCapacityLeftAndBufferSpace() ) {
				return;
			}

			addToBuffer(veh, now);
			removeVehicleFromQueue(now,veh);
		} // end while
	}

	private QVehicle removeVehicleFromQueue(final double now,final QVehicle veh2Remove) {
		//		QVehicle veh = vehQueue.poll();
		//		usedStorageCapacity -= veh.getSizeInEquivalents();

		QVehicle veh = pollFromVehQueue(veh2Remove); 

		if(seepageAllowed && isSeepModeStorageFree && veh.getVehicle().getType().getId().toString().equals(seepMode) ){
			// yyyy above line feels quite slow/consuming computer time.  Should be switched off completely when seepage is not used. kai, may'15

		} else {
			usedStorageCapacity -= veh.getSizeInEquivalents();
		}

		if ( QueueWithBuffer.HOLES ) {
			QueueWithBuffer.Hole hole = new QueueWithBuffer.Hole() ;
			double offset = length*3600./hole_speed/1000. ;
			hole.setEarliestLinkExitTime( now + 1.0*offset + 0.0*MatsimRandom.getRandom().nextDouble()*offset ) ;
			hole.setSizeInEquivalents(veh2Remove.getSizeInEquivalents());
			holes.add( hole ) ;
		}
		return veh ;
	}

	private void letVehicleArrive(final double now, QVehicle veh) {
		qLink.addParkedVehicle(veh);
		network.simEngine.letVehicleArrive(veh);
		qLink.makeVehicleAvailableToNextDriver(veh, now);
		// remove _after_ processing the arrival to keep link active
		removeVehicleFromQueue( now, veh ) ;
	}

	final int vehInQueueCount() {
		// called by test cases
		return vehQueue.size();
	}

	@Override
	public final boolean isActive() {
		if(this.fastCapacityUpdate){
		return /*(this.remainingflowCap < 0.0) // still accumulating, thus active
				|| */(!this.vehQueue.isEmpty()) || (!this.isNotOfferingVehicle()) ;
		} else {
			return (this.flowcap_accumulate.getValue() < 1.0) // still accumulating, thus active
					|| (!this.vehQueue.isEmpty()) || (!this.isNotOfferingVehicle()) ;
		}
	}

	@Override
	public final void setSignalStateAllTurningMoves( final SignalGroupState state) {
		qSignalizedItem.setSignalStateAllTurningMoves(state);

		thisTimeStepGreen  = qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation)
	}

	@Override
	public final double getSimulatedFlowCapacity() {
		return this.flowCapacityPerTimeStep;
	}

	@Override
	public final boolean isAcceptingFromUpstream() {
		boolean storageOk = usedStorageCapacity < storageCapacity ;
		if ( !QueueWithBuffer.HOLES ) {
			return storageOk ;
		}
		// (continue only if HOLES)

		if ( !storageOk ) {
			return false ;
		}
		// at this point, storage is ok, so start checking holes:
		if ( remainingHolesStorageCapacity <=0 ) { // no holes available at all; in theory, this should not happen since covered by !storageOk
			//						log.warn( " !hasSpace since no holes available ") ;
			return false ;
		} 
		return true ;
	}

	@Override
	final void recalcTimeVariantAttributes(final double now) {
		freespeedTravelTime = this.length / link.getFreespeed(now);
		// as of now, speed is NOT explicity set but pulled from the link since we assume that all lanes have the same freespeed as the
		// link
		if (Double.isNaN(freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}
		calculateFlowCapacity();
		calculateStorageCapacity();
		
		if(this.fastCapacityUpdate){
			flowcap_accumulate.setValue(flowCapacityPerTimeStep);
		}
	}

	@Override
	public final QVehicle getVehicle(final Id<Vehicle> vehicleId) {
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.buffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return null;
	}

	@Override
	final Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		vehicles.addAll(vehQueue);
		vehicles.addAll(buffer);
		return vehicles ;
	}

	@Override
	public final QVehicle popFirstVehicle() {
		QVehicle veh = removeFirstVehicle();
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		if (this.generatingEvents) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(new LaneLeaveEvent(
					now, veh.getDriver().getId(), this.link.getId(), this.getId()
					));
		}
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(
				now, veh.getDriver().getId(), this.link.getId(), veh.getId()
				));
		return veh;
	}

	final QVehicle removeFirstVehicle(){
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = buffer.poll();
		usedBufferStorageCapacity = usedBufferStorageCapacity - veh.getSizeInEquivalents();
		bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		return veh;
	}

	@Override
	public final void setSignalStateForTurningMove( final SignalGroupState state, final Id<Link> toLinkId) {
		if (!link.getToNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " +  this.id );
		}
		qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		thisTimeStepGreen = qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	@Override
	public final boolean hasGreenForToLink(final Id<Link> toLinkId) {
		if (qSignalizedItem != null){
			return qSignalizedItem.isLinkGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	final double getStorageCapacity() {
		return storageCapacity;
	}

	@Override
	final boolean isNotOfferingVehicle() {
		return buffer.isEmpty();
	}

	@Override
	public final void clearVehicles() {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : vehQueue) {
			network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			network.simEngine.getMobsim().getAgentCounter().incLost();
			network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		vehQueue.clear();

		for (QVehicle veh : buffer) {
			network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			network.simEngine.getMobsim().getAgentCounter().incLost();
			network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		buffer.clear();
		usedBufferStorageCapacity = 0;
	}

	@Override
	public final void addFromUpstream(final QVehicle veh) {

		// activate link since there is now action on it:
		qLink.activateLink();

		// reduce storage capacity by size of vehicle:
		//	usedStorageCapacity += veh.getSizeInEquivalents();

		if(isSeepModeStorageFree && veh.getVehicle().getType().getId().toString().equals(seepMode) ){
		} else {
			usedStorageCapacity += veh.getSizeInEquivalents();
		}

		// get current time:
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		// compute and set earliest link exit time:
		double linkTravelTime = this.length / this.network.simEngine.getLinkSpeedCalculator().getMaximumVelocity(veh, link, now);
		double earliestExitTime = now + linkTravelTime ;

		earliestExitTime = timeStepSize * Math.floor(earliestExitTime/timeStepSize );

		veh.setEarliestLinkExitTime(earliestExitTime);

		// In theory, one could do something like
		//		final double discretizedEarliestLinkExitTime = timeStepSize * Math.ceil(veh.getEarliestLinkExitTime()/timeStepSize);
		//		double effectiveEntryTime = now - ( discretizedEarliestLinkExitTime - veh.getEarliestLinkExitTime() ) ;
		//		double earliestExitTime = effectiveEntryTime + linkTravelTime;
		// We decided against this since this would effectively move the simulation to operating on true floating point time steps.  For example,
		// events could then have arbitrary floating point values (assuming one would use the "effectiveEntryTime" also for the event).  
		// Also, it could happen that vehicles with an earlier link exit time could be 
		// inserted and thus end up after vehicles with a later link exit time.  theresa & kai, jun'14

		veh.setCurrentLink(link);
		vehQueue.add(veh);

		if ( QueueWithBuffer.HOLES ) {
			remainingHolesStorageCapacity = remainingHolesStorageCapacity - veh.getSizeInEquivalents();
		}
	}

	@Override
	public final VisData getVisData() {
		return this.visData  ;
	}

	@Override
	public final QVehicle getFirstVehicle() {
		return this.buffer.peek() ;
	}

	@Override
	public final double getLastMovementTimeOfFirstVehicle() {
		return this.bufferLastMovedTime ;
	}

	/**
	 * Needs to be added _upstream_ of the regular stop location so that a possible second stop on the link can also be served.
	 */
	@Override
	public final void addTransitSlightlyUpstreamOfStop( final QVehicle veh) {
		this.vehQueue.addFirst(veh) ;
	}

	@Override
	public final void setSignalized( final boolean isSignalized) {
		qSignalizedItem  = new DefaultSignalizeableItem(link.getToNode().getOutLinks().keySet());
	}

	@Override
	public final void changeUnscaledFlowCapacityPerSecond( final double val, final double now ) {
		this.unscaledFlowCapacity_s = val ;
		// be defensive (might now be called twice):
		this.recalcTimeVariantAttributes(now);
	}

	@Override
	public final void changeEffectiveNumberOfLanes( final double val, final double now ) {
		this.effectiveNumberOfLanes = val ;
		// be defensive (might now be called twice):
		this.recalcTimeVariantAttributes(now);
	}

	Id<Lane> getId() {
		// need this so we can generate lane events although we do not need them here. kai, sep'13
		return this.id;
	}

	static final class Hole extends QItem {
		private double earliestLinkEndTime ;
		private double pcu;

		@Override
		final double getEarliestLinkExitTime() {
			return earliestLinkEndTime;
		}

		@Override
		final void setEarliestLinkExitTime(double earliestLinkEndTime) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}

		final double getSizeInEquivalents() {
			return this.pcu;
		}

		final void setSizeInEquivalents(double pcuFactorOfHole) {
			this.pcu = pcuFactorOfHole;
		}
	}

	class VisDataImpl implements VisData {
		private Coord upstreamCoord;
		private Coord downsteamCoord;
		private double euklideanDistance;

		@Override
		public final Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = network.simEngine.getAgentSnapshotInfoBuilder();

			double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

			// vehicles:
			if ( !buffer.isEmpty() || !vehQueue.isEmpty() ) {
				// vehicle positions are computed in snapshotInfoBuilder as a service:
				snapshotInfoBuilder.positionVehiclesAlongLine(positions, now, getAllVehicles(), length, storageCapacity
						+ bufferStorageCapacity, ((LinkImpl) link).getEuklideanDistance(), link.getFromNode().getCoord(), link
						.getToNode().getCoord(), inverseFlowCapacityPerTimeStep, link.getFreespeed(now), NetworkUtils
						.getNumberOfLanesAsInt(now, link));
			}
			if ( VIS_HOLES ) {
				// holes:
				if ( !holes.isEmpty() ) {
					double spacing = snapshotInfoBuilder.calculateVehicleSpacing(length, holes.size(), getStorageCapacity() );
					double freespeedTraveltime = length / (hole_speed*1000./3600.);
					double lastDistanceFromFromNode = Double.NaN;
					for (Hole hole : holes) {
						lastDistanceFromFromNode = createAndAddHolePositionAndReturnDistance(positions, snapshotInfoBuilder, now,
								lastDistanceFromFromNode, link, spacing, freespeedTraveltime, hole);
					}
				}
			}
			return positions ;
		}

		private double createAndAddHolePositionAndReturnDistance(final Collection<AgentSnapshotInfo> positions,
				AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder, double now, double lastDistanceFromFromNode, Link link,
				double spacing, double freespeedTraveltime, Hole veh)
		{
			double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			double distanceFromFromNode = snapshotInfoBuilder.calculateDistanceOnVectorFromFromNode2(QueueWithBuffer.this.length, spacing,
					lastDistanceFromFromNode, now, freespeedTraveltime, remainingTravelTime);
			Integer lane = 10 ;
			double speedValue = 1. ;
			if (this.upstreamCoord != null){
				snapshotInfoBuilder.positionQItem(positions, this.upstreamCoord, this.downsteamCoord,
						QueueWithBuffer.this.length, this.euklideanDistance, veh,
						distanceFromFromNode, lane, speedValue);
			} else {
				snapshotInfoBuilder.positionQItem(positions, link.getFromNode().getCoord(), link.getToNode().getCoord(),
						QueueWithBuffer.this.length, ((LinkImpl)link).getEuklideanDistance() , veh, 
						distanceFromFromNode, lane, speedValue);
			}
			return distanceFromFromNode;
		}
		void setVisInfo(Coord upstreamCoord, Coord downstreamCoord, double euklideanDistance) {
			this.upstreamCoord = upstreamCoord;
			this.downsteamCoord = downstreamCoord;
			this.euklideanDistance = euklideanDistance;
		}
	}

	private boolean seepageAllowed = false;
	private String seepMode = null; 
	private boolean isSeepModeStorageFree = false;

	private QVehicle peekFromVehQueue(){
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		if(!seepageAllowed || vehQueue.size() <= 1){
			// no seepage or no vehicle or only one vehicle in queue
			return vehQueue.peek();
		} else if(vehQueue.peek().getEarliestLinkExitTime()>now ){
			//if earliestLinkExitTime of front vehicle is not reached returns
			return vehQueue.peek();
		} else if(vehQueue.peek().getDriver().getMode().equals(seepMode)) {
			//driver is present in veh and driving seep mode
			return vehQueue.peek();
		} else {
			QVehicle returnVeh = null;
			boolean foundSeepMode = false;
			VehicleQ<QVehicle> newVehQueue = new PassingVehicleQ();
			Iterator<QVehicle> it = vehQueue.iterator();

			while(it.hasNext()){
				QVehicle veh = vehQueue.poll(); 
				if(veh.getDriver()==null) {
				} else if(veh.getEarliestLinkExitTime()<=now && veh.getDriver().getMode().equals(seepMode) && !foundSeepMode) {
					returnVeh = veh;
					newVehQueue.add(veh);
					foundSeepMode=true;
				} else {
					newVehQueue.add(veh);
				}
			}
			if(!vehQueue.isEmpty()) throw new RuntimeException("This queue should be empty at this point.");
			vehQueue.addAll(newVehQueue);
			if(returnVeh==null) return vehQueue.peek();
			else return returnVeh;
		}

	}

	private QVehicle pollFromVehQueue(QVehicle veh2Remove){
		if(vehQueue.remove(veh2Remove)){
			return veh2Remove;
		} else {
			throw new RuntimeException("Desired vehicle is not removed from vehQueue. Aborting...");
		}
	}
}
