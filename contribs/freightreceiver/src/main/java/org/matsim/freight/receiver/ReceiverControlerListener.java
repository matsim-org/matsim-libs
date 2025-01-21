/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.freight.receiver;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.freight.receiver.replanning.ReceiverStrategyManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This controller ensures that each receiver receives a cost (score) per order
 * at the end of each iteration and replans its orders based on the cost of the
 * previous iteration and past iterations.
 *
 * @author wlbean, jwjoubert
 */
class ReceiverControlerListener implements ScoringListener, IterationEndsListener,
        ReplanningListener, BeforeMobsimListener, ShutdownListener {

    final private static Logger LOG = LogManager.getLogger(ReceiverControlerListener.class);
//    private final ReceiverOrderStrategyManagerFactory strategyManagerFactory;
    private final ReceiverStrategyManager strategyManager;
    private final ReceiverScoringFunctionFactory scoringFunctionFactory;
    private final ReceiverCostAllocation costAllocation;
    private ReceiverTracker tracker;
    @Inject
    Scenario sc;

	@Inject
    private ReceiverControlerListener(ReceiverStrategyManager strategyManager, ReceiverScoringFunctionFactory scoringFunctionFactory, ReceiverCostAllocation costAllocation) {
//        this.strategyManagerFactory = strategyManagerFactory;
		this.strategyManager = strategyManager;
        this.scoringFunctionFactory = scoringFunctionFactory;
		this.costAllocation = costAllocation;
    }

    @Override
    public void notifyReplanning(final ReplanningEvent event) {
        Collection<HasPlansAndId<ReceiverPlan, Receiver>> receiverCollection = new ArrayList<>();
        Collection<HasPlansAndId<ReceiverPlan, Receiver>> receiverControlCollection = new ArrayList<>();

        for (Receiver receiver : ReceiverUtils.getReceivers(sc).getReceivers().values()) {
            if ((event.getIteration() - 1) % ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).getReceiverReplanningInterval() == 0) {
                // (= one iteration after replanning)

                receiver.setInitialCost(receiver.getSelectedPlan().getScore());
            }

            /*
             * Checks to see if a receiver is part of the grand coalition, if not, the receiver are not allowed to join
             * a coalition at any time. If the receiver is willing to collaborate, the receiver will be allowed to leave
             * and join coalitions.
             */
            if ((boolean) receiver.getAttributes().getAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER)) {
                receiverCollection.add(receiver);
            } else {
                receiverControlCollection.add(receiver);
            }

            /* Replanning for grand coalition receivers.*/
			//		GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
            //		strategy.addStrategyModule(new CollaborationStatusMutator());
            //		collaborationStratMan.addStrategy(strategy, null, 0.2);
            //		collaborationStratMan.addChangeRequest((int) Math.round((fsc.getScenario().getConfig().controler().getLastIteration())*0.8), strategy, null, 0.0);

            if (event.getIteration() % ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).getReceiverReplanningInterval() != 0) {
                // (= not in replanning iteration)
                return;
            }

            /* Run replanning for non-collaborating receivers */
            strategyManager.run(receiverControlCollection, event.getIteration(), event.getReplanningContext());

            /* Run replanning for grand coalition receivers.*/
            strategyManager.run(receiverCollection, event.getIteration(), event.getReplanningContext());
        }
    }


    /**
     * Determines the order cost at the end of each iteration.
     */
    @Override
    public void notifyScoring(ScoringEvent event) {
        if (event.getIteration() == 0) {
			/* 0th iteration. */
            this.tracker.scoreSelectedPlans();
        } else if((event.getIteration() + 1) % ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).getReceiverReplanningInterval() == 0){
			/* Receiver replanning iteration. */
			this.tracker.scoreSelectedPlans();
		} else{
			/* Non-replanning iteration. */
            for (Receiver receiver : ReceiverUtils.getReceivers(sc).getReceivers().values()) {
                double score = (double) receiver.getAttributes().getAttribute(ReceiverUtils.ATTR_RECEIVER_SCORE);
                receiver.getSelectedPlan().setScore(score);
            }
		}
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        tracker = new ReceiverTracker(scoringFunctionFactory, sc, costAllocation);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        /* Method to check the status of time windows. */
        try {
            linkReceiverTimeWindowToCarrierTourPosition();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot write receiver output, linking them to tour positions.");
        }

    }

    private void linkReceiverTimeWindowToCarrierTourPosition() throws IOException {
        LOG.info("Writing output to link receivers to the position in the carrier's tour.");
        /* Map the receivers to link Ids */
        Map<Id<Link>, Receiver> receiverLinkMap = new HashMap<>();
        Receivers receivers = ReceiverUtils.getReceivers(this.sc);
        for (Receiver receiver : receivers.getReceivers().values()) {
            Id<Link> linkId = receiver.getLinkId();
//            if (receiverLinkMap.containsKey(linkId)) {
//                LOG.error("There are multiple receivers at the same link Id. Fix how this is dealt with.");
//                throw new RuntimeException("Multiple receivers on same link.");
//            }
            receiverLinkMap.put(linkId, receiver);
        }

		try (BufferedWriter bw = IOUtils.getBufferedWriter(this.sc.getConfig().controller().getOutputDirectory() + "output_receiverInTourPlacement.csv.gz")) {
			bw.write("receiverId,twStart,twEnd,twDuration,positionInTour,product,deliveryStart,deliveryEnd");
			bw.newLine();

			for (Carrier carrier : CarriersUtils.getCarriers(this.sc).getCarriers().values()) {
				Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
				for (ScheduledTour tour : scheduledTours) {
					for (int i = 0; i < tour.getTour().getTourElements().size(); i++) {
						Tour.TourElement element = tour.getTour().getTourElements().get(i);
						if (element instanceof Tour.ShipmentBasedActivity act) {

							String shipmentId = act.getShipment().getId().toString();
							if (act.getActivityType().equalsIgnoreCase("delivery")) {
								Id<Link> linkId = act.getShipment().getDeliveryLinkId();
								if (!receiverLinkMap.containsKey(linkId)) {
									LOG.error("Woops, the carrier is delivering a shipment to an unknown receiver!");
									throw new RuntimeException("Don't know to whom delivery is.");
								}

								Tour.Leg precedingLeg = (Tour.Leg) tour.getTour().getTourElements().get(i - 1);
								Tour.Leg followingLeg = (Tour.Leg) tour.getTour().getTourElements().get(i + 1);

								String startTime = Time.writeTime(
									precedingLeg.getExpectedDepartureTime() +
										precedingLeg.getExpectedTransportTime());
								String endTime = Time.writeTime(followingLeg.getExpectedDepartureTime());

								Receiver thisReceiver = receiverLinkMap.get(linkId);
								String twStart = Time.writeTime(thisReceiver.getSelectedPlan().getTimeWindows().get(0).getStart());
								String twEnd = Time.writeTime(thisReceiver.getSelectedPlan().getTimeWindows().get(0).getEnd());
								String tw = Time.writeTime(Time.parseTime(twEnd) - Time.parseTime(twStart));
								double position = ((double) i) / ((double) tour.getTour().getTourElements().size());
								bw.write(String.format("%s,%s,%s,%s,%.4f,%s,%s,%s\n",
									thisReceiver.getId().toString(),
									twStart,
									twEnd,
									tw,
									position,
									shipmentId,
									startTime,
									endTime)
								);
							}
						}
					}
				}
			}
		}
    }


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		costAllocation.reset();
	}
}
