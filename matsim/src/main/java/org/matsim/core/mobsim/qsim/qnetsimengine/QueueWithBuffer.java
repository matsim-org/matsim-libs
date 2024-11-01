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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.DefaultFlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisVehicle;

/**
 * Separating out the "lane" functionality from the "link" functionality.
 * <p></p>
 * Design thoughts:<ul>
 * <li> In fast capacity update, the flows are not accumulated in every time step,
 * rather updated only if an agent wants to enter the link or an agent is added to buffer.
 * Improvement of 15-20% in the computational performance is observed. amit feb'16
 * (I seem to recall that in the end that statement was not consistently correct.  kai, feb'18)</li>
 * <li>Currently (feb'18), the design is such that (possibly time-dep) flowCap and nEffectiveLanes are "pushed" into the
 * class, while freeSpeed is "pulled" from the class.  This is an attempt to bridge the diverging design requirements,
 * where flowCap and nEffectiveLanes may, in the multiple lanes implementation, vary by lane (for the same link), while
 * speed uses {@link LinkSpeedCalculator}, which does not have the link freeSpeed as a parameter and thus is a "pull"
 * method.</li>
 * </ul>
 *
 * @author nagel
 */
final class QueueWithBuffer implements QLaneI, SignalizeableItem {
	private static final Logger log = LogManager.getLogger( QueueWithBuffer.class ) ;

	@Override
    public final void addFromWait(final QVehicle veh) {
        //To protect against calling addToBuffer() without calling hasFlowCapacityLeft() first.
        //This only could happen for addFromWait(), because it can be called from outside QueueWithBuffer
        if (flowcap_accumulate.getValue() <= 0.0 && veh.getVehicle().getType().getPcuEquivalents() > context.qsimConfig
                .getPcuThresholdForFlowCapacityEasing()) {
            throw new IllegalStateException("Buffer of link " + this.id + " has no space left!");
        }

        addToBuffer(veh);
	}

	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 * <p></p>
	 * I changed this into an internal class as a first step to look into acceleration (not having to keep this link active until
	 * this has accumulated to one).  There is no need to keep it this way; it just seems to make it easier to keep track of
	 * changes.  kai, sep'14
	 */
	private static class FlowcapAccumulate {
		private double timeStep = 0.;//Double.NEGATIVE_INFINITY ;
		private double value = 0. ;
		private double getTimeStep(){
			return this.timeStep;
		}
		private void setTimeStep(double now) {
			this.timeStep = now;
		}
		private double getValue() {
			return value;
		}
		private void setValue(double value ) {
			this.value = value;
		}
		private void addValue(double value1, double now) {
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
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep;
	private double remainingHolesStorageCapacity = 0.0 ;

	private final Queue<QueueWithBuffer.Hole> holes = new LinkedList<>();

	/** the last time-step the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Double.NEGATIVE_INFINITY ;

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final VehicleQ<QVehicle> vehQueue;

	private double storageCapacity;
	private double usedStorageCapacity;
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection and their capacity consumption. <br>
	 * We store the capacity consumption as it is needed twice: <br>
	 * (1) entry to the buffer (this is where it is computed and then stored) <br>
	 * (2) update of the remaining flow capacity (where we account for all vehicles that are still in the buffer since previous time steps) - see {@link #subtractConsumptionOfVehiclesThatAreAlreadyInTheBuffer()} <br>
	 */
	private final Queue<Pair<QVehicle,Double>> buffer = new LinkedList<>() ;
	/**
	 * null if the link is not signalized
	 */
	private DefaultSignalizeableItem qSignalizedItem = null ;
	/**
	 * I think that it would be good to get rid of the qLink backpointer.  Instead keep a reduced QLinkInternalInterface back
	 * pointer, and give access only to reduced number of methods (in particular not the full Link information). kai, feb'18
	 * This is now done with the {@link AbstractQLink.QLinkInternalInterface}.  kai, feb'18
	 */
	private final AbstractQLink.QLinkInternalInterface qLinkInternalInterface;
	private final Id<Lane> id;
	private static int spaceCapWarningCount = 0;
	final static double HOLE_SPEED_KM_H = 15.0;

	private final double length ;
	private double unscaledFlowCapacity_s = Double.NaN ;
	private double effectiveNumberOfLanes = Double.NaN ;

	/**
	 * Points to the latest vehicle that entered the buffer and the entry time.
	 */
	private Pair<QVehicle,Double> lastBufferEntry = null;

	/**
	 * Points to the latest vehicle that entered the queue and the entry time.
	 */
	private Pair<QVehicle,Double> lastQueueEntry = null;


	private final VisData visData = new VisDataImpl() ;
	private final NetsimEngineContext context;

	private double maxInflowUsedInQsim = Double.POSITIVE_INFINITY ;
	private double effectiveNumberOfLanesUsedInQsim = Double.POSITIVE_INFINITY ;

	private double accumulatedInflowCap = 1. ;

	private final FlowEfficiencyCalculator flowEfficiencyCalculator;

	private QueueWithBuffer(AbstractQLink.QLinkInternalInterface qlink, final VehicleQ<QVehicle> vehicleQueue, Id<Lane> laneId,
							double length, double effectiveNumberOfLanes, double flowCapacity_s, final NetsimEngineContext context,
							FlowEfficiencyCalculator flowEfficiencyCalculator) {
		// the general idea is to give this object no longer access to "everything".  Objects get back pointers (here qlink), but they
		// do not present the back pointer to the outside.  In consequence, this object can go up to qlink, but not any further. kai, mar'16
		// Now I am even trying to get rid of the full qLink back pointer (since it allows, e.g., going back to Link). kai, feb'18

//		log.setLevel(Level.DEBUG);

		this.flowEfficiencyCalculator = flowEfficiencyCalculator;
		this.qLinkInternalInterface = qlink;
		this.id = laneId ;
		this.context = context ;
		this.vehQueue = vehicleQueue ;
		this.length = length;
		this.unscaledFlowCapacity_s = flowCapacity_s ;
		this.effectiveNumberOfLanes = effectiveNumberOfLanes;

//		freespeedTravelTime = this.length / qlink.getLink().getFreespeed();
//		if (Double.isNaN(freespeedTravelTime)) {
//			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
//		}
		this.calculateFlowCapacity();
		this.calculateStorageCapacity();

		flowcap_accumulate.setValue(flowCapacityPerTimeStep);
	}

    private void addToBuffer(final QVehicle veh) {
		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12

		double now = context.getSimTimer().getTimeOfDay() ;

		double flowConsumption = (lastBufferEntry == null) ?
				getFlowCapacityConsumptionInEquivalents(veh, null, null) : getFlowCapacityConsumptionInEquivalents(veh, lastBufferEntry.getKey(), now - lastBufferEntry.getValue());
        this.flowcap_accumulate.addValue(-flowConsumption, now);

		buffer.add(new ImmutablePair<>(veh,flowConsumption));
		lastBufferEntry = new ImmutablePair<>(veh,now);

		if (buffer.size() == 1) {
			bufferLastMovedTime = now;
			// (if there is one vehicle in the buffer now, there were zero vehicles in the buffer before.  in consequence,
			// need to reset the lastMovedTime.  If, in contrast, there was already a vehicle in the buffer before, we can
			// use the lastMovedTime that was (somehow) computed for that vehicle.)
		}
		final QNodeI toNode = qLinkInternalInterface.getToNodeQ();
		if ( toNode instanceof AbstractQNode ) {
			((AbstractQNode) toNode).activateNode();
			// yy for an "upstream" QLane, this activates the toNode too early.  Yet I think I founds this
			// also in the original QLane code.  kai, sep'13
		}

	}

    /**
     * Move vehicles from link to buffer, according to buffer capacity and
     * departure time of vehicle. Also removes vehicles from lane if the vehicle
     * arrived at its destination.
     */
    private void moveQueueToBuffer() {
        double now = context.getSimTimer().getTimeOfDay();

        QVehicle veh;
        while ((veh = peekFromVehQueue()) != null) {
            //we have an original QueueLink behaviour
            if (veh.getEarliestLinkExitTime() > now) {
                return;
            }

            MobsimDriverAgent driver = veh.getDriver();

            if (driver instanceof TransitDriverAgent) {
                HandleTransitStopResult handleTransitStop = qLinkInternalInterface.handleTransitStop(
                        now, veh, (TransitDriverAgent) driver, this.qLinkInternalInterface.getId()
												    );
                if (handleTransitStop == HandleTransitStopResult.accepted) {
                    // vehicle has been accepted into the transit vehicle queue of the link.
                    removeVehicleFromQueue(veh);
                    continue;
                } else if (handleTransitStop == HandleTransitStopResult.rehandle) {
                    continue; // yy why "continue", and not "break" or "return"?  Seems to me that this
                    // is currently only working because qLink.handleTransitStop(...) also increases the
                    // earliestLinkExitTime for the present vehicle.  kai, oct'13
                    // zz From my point of view it is exactly like described above. dg, mar'14
//				} else if (handleTransitStop == HandleTransitStopResult.continue_driving) {
                    // Do nothing, but go on..
                }
            }

            // Check if veh has reached destination:
            if (driver.isWantingToArriveOnCurrentLink()) {
                if ( qLinkInternalInterface.letVehicleArrive(veh )) {
                    // remove _after_ processing the arrival to keep link active:
                    removeVehicleFromQueue(veh);
                    continue;
                } else { // The current vehicle is not allowed to arrive, so it will block the link
                    return;
                }
            }

            /* is there still any flow capacity left? */
            if (!hasFlowCapacityLeft(veh)) {
                return;
            }

            removeVehicleFromQueue(veh);
            addToBuffer(veh); //isn't prevVehicle always equal to veh here!?

            if (context.qsimConfig.isRestrictingSeepage()
                    && context.qsimConfig.getLinkDynamics() == LinkDynamics.SeepageQ
                    && context.qsimConfig.getSeepModes().contains(veh.getDriver().getMode())) {
                noOfSeepModeBringFwd++;
            }
        } // end while
    }

	@Override
	public final boolean isAcceptingFromWait(QVehicle veh) {
		return this.hasFlowCapacityLeft(veh) ;
	}

	private boolean hasFlowCapacityLeft(VisVehicle veh) {
		if(context.qsimConfig.isUsingFastCapacityUpdate() ){
			updateFastFlowAccumulation();
		}

		return flowcap_accumulate.getValue() > 0.0 || veh.getVehicle().getType()
				.getPcuEquivalents() <= context.qsimConfig.getPcuThresholdForFlowCapacityEasing();
	}

	private void updateFastFlowAccumulation(){
		double now = context.getSimTimer().getTimeOfDay() ;

		double remainingFlowCapThisTimeStep = subtractConsumptionOfVehiclesThatAreAlreadyInTheBuffer();

		if( this.flowcap_accumulate.getTimeStep() < now
				&& this.flowcap_accumulate.getValue() < remainingFlowCapThisTimeStep){

			double timeSteps = (now - flowcap_accumulate.getTimeStep()) / context.qsimConfig.getTimeStepSize();
			double accumulateFlowCap = timeSteps * flowCapacityPerTimeStep;
			double newFlowCap = Math.min(flowcap_accumulate.getValue() + accumulateFlowCap,
					remainingFlowCapThisTimeStep);

			flowcap_accumulate.setValue(newFlowCap);
			flowcap_accumulate.setTimeStep( now );
		}
	}

	private void updateSlowFlowAccumulation(){
		double remainingFlowCapThisTimeStep = subtractConsumptionOfVehiclesThatAreAlreadyInTheBuffer();

		if (this.thisTimeStepGreen
				&& this.flowcap_accumulate.getValue() < remainingFlowCapThisTimeStep){
			double newFlowCap = Math.min(flowcap_accumulate.getValue() + flowCapacityPerTimeStep,
					remainingFlowCapThisTimeStep);
			flowcap_accumulate.setValue(newFlowCap);
		}
	}

	private double subtractConsumptionOfVehiclesThatAreAlreadyInTheBuffer() {
		double remainingFlowCapThisTimeStep = flowCapacityPerTimeStep;
		for (Pair<QVehicle,Double> vehEfficiencyPair : buffer) {
			// Subtract size of vehicles that are already in the buffer (from previous time steps)
			remainingFlowCapThisTimeStep -= vehEfficiencyPair.getValue();
		}
		return remainingFlowCapThisTimeStep;
	}

	@Override
	public final void initBeforeSimStep() {
		if(!context.qsimConfig.isUsingFastCapacityUpdate() ){
			updateSlowFlowAccumulation();
		}
	}
	private static int wrnCnt=0 ;
	private void calculateFlowCapacity() {
		// the following is not looking at time because it simply assumes that the lookups are "now". kai, feb'18
		// I am currently not sure if this statement is correct. kai, feb'18

		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		flowCapacityPerTimeStep = unscaledFlowCapacity_s * context.qsimConfig.getTimeStepSize() * context.qsimConfig.getFlowCapFactor() ;
		inverseFlowCapacityPerTimeStep = 1.0 / flowCapacityPerTimeStep;

		// start with the base assumption, might be adjusted below depending on the traffic dynamics
		this.effectiveNumberOfLanesUsedInQsim = this.effectiveNumberOfLanes;
		this.maxInflowUsedInQsim = this.flowCapacityPerTimeStep;

		switch (context.qsimConfig.getTrafficDynamics()) {
			case queue:
			case withHoles:
				break;
			case kinematicWaves:
				// uncongested branch: q = vmax * rho
				// congested branch: q = vhole * (rhojam - rho)
				// equal: rho * (vmax + vhole) = vhole * rhojam
				// rho(qmax) = vhole * rhojam / (vmax + vhole)
				// qmax = vmax * rho(qmax) = rhojam / (1/vhole + 1/vmax) ;

				// yyyyyy this should possibly be getFreespeed(now). But if that's the case, then maxFlowFromFdiag would
				// also have to be re-computed with each freespeed change. kai, feb'18

				final double maxFlowFromFdiag = (this.effectiveNumberOfLanes/context.effectiveCellSize) / ( 1./(HOLE_SPEED_KM_H/3.6) + 1/this.qLinkInternalInterface.getFreespeed() ) ;
				final double minimumNumberOfLanesFromFdiag = this.flowCapacityPerTimeStep * context.effectiveCellSize * ( 1./(HOLE_SPEED_KM_H/3.6) + 1/this.qLinkInternalInterface.getFreespeed() );

				QSimConfigGroup.InflowCapacitySetting inflowCapacitySetting = context.qsimConfig.getInflowCapacitySetting();

				if(inflowCapacitySetting == QSimConfigGroup.InflowCapacitySetting.MAX_CAP_FOR_ONE_LANE){
					if (wrnCnt<10) {
						wrnCnt++ ;
						log.warn("you are using the maximum capacity for one lane as the inflow capacity. This is the old standard behavior of the qsim and probably leads to wrong results " +
								" as it does not respect the actual number of lanes nor the user-defined flow capacity. Please consider using" +
								"InflowCapacitySetting.INCREASE_NUMBER_OF_LANES or InflowCapacitySetting.REDUCE_INFLOW_CAPACITY instead.");
					}
					if ( wrnCnt==5 ) { //this verbose warning is only given 5 times
						log.warn( Gbl.FUTURE_SUPPRESSED ) ;
					}

					this.maxInflowUsedInQsim = (1/context.effectiveCellSize) / ( 1./(HOLE_SPEED_KM_H/3.6) + 1/this.qLinkInternalInterface.getFreespeed() ) ;
					// write out the modified qsim behavior as link attribute
					qLinkInternalInterface.getLink().getAttributes().putAttribute("maxInflowUsedInQsim", 3600* maxInflowUsedInQsim /context.qsimConfig.getTimeStepSize() );

				} else  {
					if ( maxFlowFromFdiag < flowCapacityPerTimeStep ){ //warnings
						if (wrnCnt<10) {
							wrnCnt++ ;
							log.warn( "max flow from fdiag < flow cap in network file; linkId=" + qLinkInternalInterface.getId() +
									"; network file flow cap/h=" + 3600.*flowCapacityPerTimeStep/context.qsimConfig.getTimeStepSize() +
									"; max flow from fdiag/h=" + 3600*maxFlowFromFdiag/context.qsimConfig.getTimeStepSize() ) ;

							log.warn( "number of lanes from fdiag > number of lanes in network file; linkId=" + qLinkInternalInterface.getId() +
									"; number of lanes in network file=" + this.effectiveNumberOfLanes +
									"; number of lanes from fdiag=" + minimumNumberOfLanesFromFdiag ) ;

							if ( wrnCnt==10 ) {
								log.warn( Gbl.FUTURE_SUPPRESSED ) ;
							}
						}
						if (inflowCapacitySetting == QSimConfigGroup.InflowCapacitySetting.INFLOW_FROM_FDIAG) {
							if (wrnCnt<10) {
								log.warn("The flow capacity will be reduced. See link attribute 'maxInflowUsedInQsim' written into the output network.");
							}
						} else if (inflowCapacitySetting == QSimConfigGroup.InflowCapacitySetting.NR_OF_LANES_FROM_FDIAG) {
							if (wrnCnt<10) {
								log.warn("The number of lanes will be increased. See link attribute 'effectiveNumberOfLanesUsedInQsim' written into the output network.");
							}
						}
					}
					// now either correct the flow capacity or the number of lanes!
					if (inflowCapacitySetting == QSimConfigGroup.InflowCapacitySetting.INFLOW_FROM_FDIAG) {
						this.maxInflowUsedInQsim = maxFlowFromFdiag;
						// write out the modified qsim behavior as link attribute
						qLinkInternalInterface.getLink().getAttributes().putAttribute("maxInflowUsedInQsim", 3600* maxInflowUsedInQsim /context.qsimConfig.getTimeStepSize() );
					} else if (inflowCapacitySetting == QSimConfigGroup.InflowCapacitySetting.NR_OF_LANES_FROM_FDIAG) {
						this.effectiveNumberOfLanesUsedInQsim = minimumNumberOfLanesFromFdiag;
						// write out the modified qsim behavior as link attribute
						qLinkInternalInterface.getLink().getAttributes().putAttribute("effectiveNumberOfLanesUsedInQsim", effectiveNumberOfLanesUsedInQsim );
					} else {
						throw new RuntimeException("The approach "+ inflowCapacitySetting.toString()+" is not implemented yet.");
					}
				}
				break;

			default: throw new RuntimeException("The traffic dynamics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
//		log.debug( "linkId=" + this.qLink.getLink().getId() + "; flowCapPerTimeStep=" + flowCapacityPerTimeStep +
//						   "; invFlowCapPerTimeStep=" + inverseFlowCapacityPerTimeStep + "; maxFlowFromFdiag=" + maxFlowFromFdiag ) ;

	}

	private void calculateStorageCapacity() {
		// The following is not adjusted for time-dependence!! kai, apr'16
		// No, I think that it simply assumes that the lookups are "now". kai, feb'18
//		double now = context.getSimTimer().getTimeOfDay() ;

		// first guess at storageCapacity:
		storageCapacity = this.length * this.effectiveNumberOfLanesUsedInQsim / context.effectiveCellSize * context.qsimConfig.getStorageCapFactor() ;
//		storageCapacity = this.length * this.qLink.getLink().getNumberOfLanes(now) / context.effectiveCellSize * context.qsimConfig.getStorageCapFactor() ;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		storageCapacity = Math.max(storageCapacity, getBufferStorageCapacity());

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the above spaceCap to handle the flowCap.
		 * Example: Assume freeSpeedTravelTime (aka freeTravelDuration) is 2 seconds. Than we need the spaceCap = TWO
		 * times the flowCap per second to handle the flowCap.
		 *
		 * Will base these computations (for the time being) on the standard free speed; i.e. reductions in free speed
		 * will also reduce the maximum flow.
		 */
		double freespeedTravelTime = this.length / qLinkInternalInterface.getFreespeed();
		// yyyyyy this should possibly be getFreespeed(now). But if that's the case, then storageCap would
		// also have to be re-computed with each freespeed change. kai, feb'18
		if (Double.isNaN(freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}

		//this assumes that vehicles have the flowEfficiencyFactor of 1.0; the actual flow can be different
		double tempStorageCapacity = freespeedTravelTime * unscaledFlowCapacity_s * context.qsimConfig.getFlowCapFactor();
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

			// write out the modified qsim behavior as link attribute
			qLinkInternalInterface.getLink().getAttributes().putAttribute("storageCapacityUsedInQsim", storageCapacity );
		}

		/* About minStorCapForHoles:
		 * () uncongested branch is q(rho) = rho * v_max
		 * () congested branch is q(rho) = (rho - rho_jam) * v_holes
		 * () rho_maxflow is where these two meet, resulting in rho_maxflow = v_holes * rho_jam / ( v_holes + v_max )
		 * () max flow is q(rho_maxflow), resulting in v_max * v_holes * rho_jam / ( v_holes + v_max )
		 * () Since everything else is given, rho_jam needs to be large enough so that q(rho_maxflow) can reach capacity, resulting in
		 *    rho_jam >= capacity * (v_holes + v_max) / (v_max * v_holes) ;
		 * () In consequence, storage capacity needs to be larger than curved_length * rho_jam .
		 *
		 */

		switch (context.qsimConfig.getTrafficDynamics()) {
			case queue:
				break;
			case withHoles:
			case kinematicWaves:
				//			final double minStorCapForHoles = 2. * flowCapacityPerTimeStep * context.getSimTimer().getSimTimestepSize();
				final double freeSpeed = qLinkInternalInterface.getFreespeed() ; // yyyyyy not clear why this is not time-dep. kai, feb'18
				final double holeSpeed = HOLE_SPEED_KM_H/3.6;
				final double minStorCapForHoles = length * flowCapacityPerTimeStep * (freeSpeed + holeSpeed) / freeSpeed / holeSpeed ;
				//			final double minStorCapForHoles = 2.* length * flowCapacityPerTimeStep * (freeSpeed + holeSpeed) / freeSpeed / holeSpeed ;
				// I have no idea why the factor 2 needs to be there?!?! kai, apr'16
				// I just removed the factor of 2 ... seems to work now without.  kai, may'16
				// yyyyyy (not thought through for TS != 1sec!  (should use flow cap per second) kai, apr'16)
				if ( storageCapacity < minStorCapForHoles ) {
					if ( spaceCapWarningCount <= 10 ) {
						log.warn("storage capacity not sufficient for holes; increasing from " + storageCapacity + " to " + minStorCapForHoles ) ;
						QueueWithBuffer.spaceCapWarningCount++;
					}
					storageCapacity = minStorCapForHoles ;
					// write out the modified qsim behavior as link attribute
					qLinkInternalInterface.getLink().getAttributes().putAttribute("storageCapacityUsedInQsim", storageCapacity );
				}

				remainingHolesStorageCapacity = this.storageCapacity;
				// yyyy how is this.storageCapacity supposed to have a value here?  (It might just be zero, and
				// maybe this is the correct value, but the code is not very expressive.)  kai, mar'17
				// i think, at this location, this explains everything. amit mar'17
				break;
			default: throw new RuntimeException("The traffic dynmics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
	}

	private double getBufferStorageCapacity() {
		return flowCapacityPerTimeStep;//this assumes that vehicles have the flowEfficiencyFactor of 1.0
	}

	@Override
	public final boolean doSimStep( ) {
		switch (context.qsimConfig.getTrafficDynamics()) {
			case queue:
				break;
			case withHoles:
				this.processArrivalOfHoles( ) ;
				break;
			case kinematicWaves:
				this.accumulatedInflowCap = Math.min(accumulatedInflowCap + maxInflowUsedInQsim, maxInflowUsedInQsim);
				this.processArrivalOfHoles( ) ;
				break;
			default: throw new RuntimeException("The traffic dynmics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
		this.moveQueueToBuffer();
		return true ;
	}

	private void processArrivalOfHoles() {
		double now = context.getSimTimer().getTimeOfDay() ;
		while ( this.holes.size()>0 && this.holes.peek().getEarliestLinkExitTime() < now ) {
			Hole hole = this.holes.poll(); // ???
            this.remainingHolesStorageCapacity += hole.getSizeInEquivalents();
        }
    }

    @Override
    public final void addFromUpstream(final QVehicle veh) {
        double now = context.getSimTimer().getTimeOfDay();

        if (this.context.qsimConfig.isUseLanes()) {
            if (hasMoreThanOneLane()) {
                this.context.getEventsManager().processEvent(new LaneEnterEvent(now, veh.getId(), this.qLinkInternalInterface.getId(), this.getId()) );
            }
        }

        // activate link since there is now action on it:
        qLinkInternalInterface.activateLink();

        if (context.qsimConfig.isSeepModeStorageFree() && context.qsimConfig.getSeepModes().contains(veh.getVehicle().getType().getId().toString())) {
            // do nothing
        } else {
            usedStorageCapacity += veh.getSizeInEquivalents();
        }

        // compute and set earliest link exit time:
//		double linkTravelTime = this.length / this.linkSpeedCalculator.getMaximumVelocity(veh, qLink.getLink(), now);
        double linkTravelTime = this.length / this.qLinkInternalInterface.getMaximumVelocityFromLinkSpeedCalculator(veh, now );
        linkTravelTime = context.qsimConfig.getTimeStepSize() * Math.floor(linkTravelTime / context.qsimConfig.getTimeStepSize());

        veh.setEarliestLinkExitTime(now + linkTravelTime);

        // In theory, one could do something like
        //		final double discretizedEarliestLinkExitTime = timeStepSize * Math.ceil(veh.getEarliestLinkExitTime()/timeStepSize);
        //		double effectiveEntryTime = now - ( discretizedEarliestLinkExitTime - veh.getEarliestLinkExitTime() ) ;
        //		double earliestExitTime = effectiveEntryTime + linkTravelTime;
        // We decided against this since this would effectively move the simulation to operating on true floating point time steps.  For example,
        // events could then have arbitrary floating point values (assuming one would use the "effectiveEntryTime" also for the event).
        // Also, it could happen that vehicles with an earlier link exit time could be
        // inserted and thus end up after vehicles with a later link exit time.  theresa & kai, jun'14

//		veh.setCurrentLink(qLink.getLink());
        this.qLinkInternalInterface.setCurrentLinkToVehicle(veh );
        vehQueue.add(veh);

        switch (context.qsimConfig.getTrafficDynamics()) {
            case queue:
                break;
            case withHoles:
                this.remainingHolesStorageCapacity -= veh.getSizeInEquivalents();
                break;
            case kinematicWaves:
                this.remainingHolesStorageCapacity -= veh.getSizeInEquivalents();
				double flowConsumption = (lastQueueEntry == null) ?
						getFlowCapacityConsumptionInEquivalents(veh, null, null) : getFlowCapacityConsumptionInEquivalents(veh, lastQueueEntry.getKey(), now - lastQueueEntry.getValue());
                this.accumulatedInflowCap -= flowConsumption;
                break;
            default:
                throw new RuntimeException("The traffic dynamics " + context.qsimConfig.getTrafficDynamics() + " is not implemented yet.");
		}

		lastQueueEntry = new ImmutablePair<>(veh, now);
	}

	private void removeVehicleFromQueue(final QVehicle veh2Remove) {
		double now = context.getSimTimer().getTimeOfDay() ;


		//		QVehicle veh = vehQueue.poll();
		//		usedStorageCapacity -= veh.getSizeInEquivalents();

		QVehicle veh = pollFromVehQueue(veh2Remove);

		if(context.qsimConfig.getLinkDynamics()==LinkDynamics.SeepageQ
				&& context.qsimConfig.isSeepModeStorageFree()
				&& context.qsimConfig.getSeepModes().contains(veh.getVehicle().getType().getId().toString()) ){
			// do nothing
		} else {
			usedStorageCapacity -= veh.getSizeInEquivalents();
		}

		switch (context.qsimConfig.getTrafficDynamics()) {
			case queue:
				break;
			case withHoles:
			case kinematicWaves:
				QueueWithBuffer.Hole hole = new QueueWithBuffer.Hole() ;

				//			double offset = this.storageCapacity/this.flowCapacityPerTimeStep ;
			/* NOTE: Start with completely full link, i.e. N_storageCap cells filled.  Now make light at end of link green, discharge with
			 * flowCapPerTS.  After N_storageCap/flowCapPerTS, the link is empty.  Which also means that the holes must have reached
			 * the upstream end of the link.  I.e. speed_holes = length / (N_storageCap/flowCap) and
			 * ttime_holes = lenth/speed = N_storCap/flowCap.
			 * Say length=75m, storCap=10, flowCap=1/2sec.  offset = 20sec.  75m/20sec = 225m/1min = 13.5km/h so this is normal.
			 * Say length=75m, storCap=20, flowCap=1/2sec.  offset = 40sec.  ... = 6.75km/h ... to low.  Reason: unphysical parameters.
			 * (Parameters assume 2-lane road, which should have discharge of 1/sec.  Or we have lots of  tuk tuks, which have only half a vehicle
			 * length.  Thus we incur the reaction time twice as often --> half speed of holes.
			 */

				//			double nLanes = 2. * flowCapacityPerTimeStep ; // pseudo-lanes
				//			double ttimeOfHoles = 0.1 * this.storageCapacity/this.flowCapacityPerTimeStep/nLanes ;

				// The calculation of the earliest exit time looked like the formula below. It looks like someone tried to include some randomness,
				// but the random part was multiplied with zero, therefore I removed it. Janek oct' 24
				// now + 1.0*ttimeOfHoles + 0.0*MatsimRandom.getRandom().nextDouble()*ttimeOfHoles
				var holeTravelTime = length * 3.6 / HOLE_SPEED_KM_H;
				var earliestExitTime = now + holeTravelTime;
				hole.setEarliestLinkExitTime(earliestExitTime) ;
				hole.setSizeInEquivalents(veh2Remove.getSizeInEquivalents());
				holes.add( hole ) ;
				break;
			default: throw new RuntimeException("The traffic dynmics "+context.qsimConfig.getTrafficDynamics()+" is not implemented yet.");
		}
	}

	@Override
	public final boolean isActive() {
		if( context.qsimConfig.isUsingFastCapacityUpdate() ){
			return (!this.vehQueue.isEmpty())
					|| (!this.isNotOfferingVehicle() && context.qsimConfig.isUseLanes()) // if lanes, the buffer needs to be active in order to move vehicles over an internal node
					|| ( !this.holes.isEmpty() ) ;
		} else {
			return (this.flowcap_accumulate.getValue() < flowCapacityPerTimeStep) // still accumulating, thus active
					|| (!this.vehQueue.isEmpty()) // vehicles are on link, thus active
					|| (!this.isNotOfferingVehicle() && context.qsimConfig.isUseLanes()) // if lanes, the buffer needs to be active in order to move vehicles over an internal node
					|| ( !this.holes.isEmpty() ); // need to process arrival of holes
		}
	}

	@Override
	public final void setSignalStateAllTurningMoves( final SignalGroupState state) {
		qSignalizedItem.setSignalStateAllTurningMoves(state);

		thisTimeStepGreen  = qSignalizedItem.hasGreenForAllToLinks();
		// (this is only for capacity accumulation)
	}

	@Override
	public final double getSimulatedFlowCapacityPerTimeStep() {
		return this.flowCapacityPerTimeStep;
	}

	@Override
	public final boolean isAcceptingFromUpstream() {
		boolean storageOk = usedStorageCapacity < storageCapacity ;

		if ( context.qsimConfig.getTrafficDynamics()==TrafficDynamics.queue )  {
			return storageOk ;
		}
		// (continue only if HOLES and/or inflow constraint)

		if ( context.qsimConfig.getTrafficDynamics() != TrafficDynamics.queue
				&& remainingHolesStorageCapacity <= 0. ) {
			// check the holes storage capacity if using holes only (amit, Aug 2016)
			return false ;
		}
		// remainingHolesStorageCapacity is:
		// * initialized at linkStorageCapacity
		// * reduced by entering vehicles
		// * increased by holes arriving at upstream end of link

		if ( context.qsimConfig.getTrafficDynamics() != TrafficDynamics.kinematicWaves) {
			return true ;
		}

		return this.accumulatedInflowCap > 0;

	}

	@Override
	public void recalcTimeVariantAttributes() {
		// not speed, since that is looked up anyways.
		// yy might also make flow and storage self-detecting changes (not really that
		// much more expensive). kai, feb'18

//		log.debug("just entered recalcTimeVariantAttributes; now=" + this.context.getSimTimer().getTimeOfDay() ) ;

		calculateFlowCapacity();
		calculateStorageCapacity();
		flowcap_accumulate.setValue(flowCapacityPerTimeStep);
	}

//	@Override
//	public final void changeSpeedMetersPerSecond( final double val ) {
//		this.freespeedTravelTime = this.length / val ;
//		if (Double.isNaN(freespeedTravelTime)) {
//			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
//		}
//	}

	@Override
	public final QVehicle getVehicle(final Id<Vehicle> vehicleId) {
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (Pair<QVehicle,Double> vehEfficiencyPair : this.buffer) {
			if (vehEfficiencyPair.getKey().getId().equals(vehicleId))
				return vehEfficiencyPair.getKey();
		}
		return null;
	}

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		/* since it is an instance of arrayList, insertion order is maintained. Thus, correcting the order or insertion.
		 * It will be more complicated for passingQueue. amit feb'16
		 */
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		for (Pair<QVehicle, Double> pair : buffer) {
			vehicles.add(pair.getKey());
		}
		vehicles.addAll(vehQueue);
		return vehicles ;
	}

	@Override
	public final QVehicle popFirstVehicle() {
		double now = context.getSimTimer().getTimeOfDay() ;
		QVehicle veh = removeFirstVehicle();
		if (this.context.qsimConfig.isUseLanes() ) {
			if (  hasMoreThanOneLane() ) {
				this.context.getEventsManager().processEvent(new LaneLeaveEvent( now, veh.getId(), this.qLinkInternalInterface.getId(), this.getId() ) );
			}
		}
		return veh;
	}

	private final QVehicle removeFirstVehicle(){
		double now = context.getSimTimer().getTimeOfDay() ;
		QVehicle veh = buffer.poll().getKey();
		bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		if( context.qsimConfig.isUsingFastCapacityUpdate() ) {
			flowcap_accumulate.setTimeStep(now - context.qsimConfig.getTimeStepSize());
		}
		return veh;
	}

	@Override
	public final void setSignalStateForTurningMove( final SignalGroupState state, final Id<Link> toLinkId) {
		if (!qLinkInternalInterface.getToNode().getOutLinks().containsKey(toLinkId )){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " +  this.id );
		}
		qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		thisTimeStepGreen = qSignalizedItem.hasGreenForAllToLinks();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	@Override
	public final boolean hasGreenForToLink(final Id<Link> toLinkId) {
		if (qSignalizedItem != null){
			return qSignalizedItem.hasGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	public boolean hasGreenForAllToLinks() {
		if (qSignalizedItem != null) {
			return qSignalizedItem.hasGreenForAllToLinks();
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	public final double getStorageCapacity() {
		return storageCapacity;
	}

	@Override
	public final boolean isNotOfferingVehicle() {
		return buffer.isEmpty();
	}

	@Override
	public final void clearVehicles() {
		// yyyyyy right now it seems to me that one should rather just abort the agents and have the framework take care of the rest. kai, mar'16

		double now = context.getSimTimer().getTimeOfDay() ;

		for (QVehicle veh : vehQueue) {
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		vehQueue.clear();

		for (Pair<QVehicle,Double> bufferEntry : buffer) {
			QVehicle veh = bufferEntry.getKey();
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));

			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		buffer.clear();

		holes.clear();
		this.remainingHolesStorageCapacity = this.storageCapacity;
    }

    private double getFlowCapacityConsumptionInEquivalents(QVehicle vehicle, QVehicle prevVehicle, Double timeDiff) {
        double flowEfficiency = flowEfficiencyCalculator.calculateFlowEfficiency(vehicle, prevVehicle, timeDiff, qLinkInternalInterface.getLink(), id );
        return vehicle.getSizeInEquivalents() / flowEfficiency;
	}

	private boolean hasMoreThanOneLane() {
		return this.qLinkInternalInterface.getAcceptingQLane() != this.qLinkInternalInterface.getOfferingQLanes().get(0 );
		// this works independent from sorting since if there is only one lane, then it has to be the one to be returned by
		// getOfferingQLanes().get(0), and it is also the same as the accepting QLane.  If, however, "lanes" is used,
		// there are at least two lanes in sequence, so the accepting lane is never the same as any of the offering lanes, and
		// this will always return false independent from sorting.  kai/theresa, dec'16
	}

	@Override
	public final QLaneI.VisData getVisData() {
		return this.visData  ;
	}

	@Override
	public final QVehicle getFirstVehicle() {
		if (this.buffer.isEmpty()) {
			return this.vehQueue.peek();
		}
		return this.buffer.peek().getKey() ;
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
		qSignalizedItem  = new DefaultSignalizeableItem( qLinkInternalInterface.getToNode().getOutLinks().keySet());
	}

	@Override
	public final void changeUnscaledFlowCapacityPerSecond( final double val ) {
		this.unscaledFlowCapacity_s = val ;
		// be defensive (might now be called twice):
		this.recalcTimeVariantAttributes();
	}

	@Override
	public final void changeEffectiveNumberOfLanes( final double val ) {
		this.effectiveNumberOfLanes = val ;
		// be defensive (might now be called twice):
		this.recalcTimeVariantAttributes();
	}

	@Override public Id<Lane> getId() {
		return this.id;
	}

	static final class Hole implements QItem {
		private double earliestLinkEndTime ;
		private double pcu;

		@Override
		public final double getEarliestLinkExitTime() {
			return earliestLinkEndTime;
		}

		@Override
		public final void setEarliestLinkExitTime( double earliestLinkEndTime ) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}

		@Override
		public final double getSizeInEquivalents() {
			return this.pcu;
		}

		final void setSizeInEquivalents(double pcuFactorOfHole) {
			this.pcu = pcuFactorOfHole;
		}

		@Override
		public Vehicle getVehicle() {
			return null ;
		}

		@Override
		public MobsimDriverAgent getDriver() {
			return null ;
		}

		@Override
		public Id<Vehicle> getId() {
			return null ;
		}
	}

	class VisDataImpl implements QLaneI.VisData {
		private Coord upstreamCoord;
		private Coord downstreamCoord;

		@Override
		public final Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions, double now) {
			if ( !buffer.isEmpty() || !vehQueue.isEmpty() || !holes.isEmpty() ) {
				Gbl.assertNotNull(positions);
				Gbl.assertNotNull( context.snapshotInfoBuilder );
				if ( this.upstreamCoord==null ) {
					this.upstreamCoord = qLinkInternalInterface.getFromNode().getCoord() ;
				}
				if ( this.downstreamCoord==null ) {
					this.downstreamCoord = qLinkInternalInterface.getToNode().getCoord() ;
				}
				// vehicle positions are computed in snapshotInfoBuilder as a service:
				positions = context.snapshotInfoBuilder.positionVehiclesAlongLine(
						positions,
						now,
						getAllVehicles(),
						length,
						storageCapacity + getBufferStorageCapacity(),
						this.upstreamCoord,
						this.downstreamCoord,
						inverseFlowCapacityPerTimeStep,
						qLinkInternalInterface.getFreespeed(now ),
//						NetworkUtils.getNumberOfLanesAsInt(now, qLink.getLink()),
						qLinkInternalInterface.getNumberOfLanesAsInt(now ) ,
						holes,
						qLinkInternalInterface
						);

			}
			return positions ;
		}

		void setVisInfo(Coord upstreamCoord, Coord downstreamCoord) {
			this.upstreamCoord = upstreamCoord;
			this.downstreamCoord = downstreamCoord;
		}
	}

	private int noOfSeepModeBringFwd = 0;

	private QVehicle peekFromVehQueue(){

		QVehicle returnVeh = vehQueue.peek();

		if( context.qsimConfig.getLinkDynamics()==LinkDynamics.SeepageQ ) {
			double now = context.getSimTimer().getTimeOfDay();

			int maxSeepModeAllowed = 4;
			if( context.qsimConfig.isRestrictingSeepage() && noOfSeepModeBringFwd == maxSeepModeAllowed) {
				noOfSeepModeBringFwd = 0;
				return returnVeh;
			}

			VehicleQ<QVehicle> newVehQueue = new PassingVehicleQ();
			newVehQueue.addAll(vehQueue);

			Iterator<QVehicle> it = newVehQueue.iterator();

			while(it.hasNext()){
				QVehicle veh = newVehQueue.poll();
				if( veh.getEarliestLinkExitTime()<=now && context.qsimConfig.getSeepModes().contains(veh.getDriver().getMode()) ) {
					returnVeh = veh;
					break;
				}
			}
		}
		return returnVeh;
	}

	private QVehicle pollFromVehQueue(QVehicle veh2Remove){
		if(vehQueue.remove(veh2Remove)){
			return veh2Remove;
		} else {
			throw new RuntimeException("Desired vehicle is not removed from vehQueue. Aborting...");
		}
	}

	@Override
	public double getLoadIndicator() {
		return usedStorageCapacity;
    }

    static final class Builder implements LaneFactory {
        private final NetsimEngineContext context;
        private VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ();
        private Id<Lane> id = null;
        private Double length = null;
        private Double effectiveNumberOfLanes = null;
        private Double flowCapacity_s = null;
        private FlowEfficiencyCalculator flowEfficiencyCalculator;

        Builder(final NetsimEngineContext context) {
            this.context = context;
            if (context.qsimConfig.getLinkDynamics() == QSimConfigGroup.LinkDynamics.PassingQ ||
                    context.qsimConfig.getLinkDynamics() == QSimConfigGroup.LinkDynamics.SeepageQ) {
                this.vehicleQueue = new PassingVehicleQ();
            }
        }

        void setVehicleQueue(VehicleQ<QVehicle> vehicleQueue) {
            this.vehicleQueue = vehicleQueue;
        }

        void setLaneId(Id<Lane> id) {
            this.id = id;
        }

        void setLength(Double length) {
            this.length = length;
        }

        void setEffectiveNumberOfLanes(Double effectiveNumberOfLanes) {
            this.effectiveNumberOfLanes = effectiveNumberOfLanes;
        }

        void setFlowCapacity_s(Double flowCapacity_s) {
            this.flowCapacity_s = flowCapacity_s;
        }

        void setFlowEfficiencyCalculator(FlowEfficiencyCalculator flowEfficiencyCalculator) {
            this.flowEfficiencyCalculator = flowEfficiencyCalculator;
        }

        @Override
        public QueueWithBuffer createLane(AbstractQLink qLink) {
            // a number of things I cannot configure before I have the qlink:
            if (id == null) {
                id = Id.create(qLink.getLink().getId(), Lane.class);
            }
            if (length == null) {
                length = qLink.getLink().getLength();
            }
            if (effectiveNumberOfLanes == null) {
                effectiveNumberOfLanes = qLink.getLink().getNumberOfLanes();
            }
            if (flowCapacity_s == null) {
                flowCapacity_s = qLink.getLink().getFlowCapacityPerSec();
            }
            if (flowEfficiencyCalculator == null) {
                flowEfficiencyCalculator = new DefaultFlowEfficiencyCalculator();
            }
            return new QueueWithBuffer(qLink.getInternalInterface(), vehicleQueue, id, length, effectiveNumberOfLanes, flowCapacity_s, context, flowEfficiencyCalculator ) ;
		}
	}

}
