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

package org.matsim.contrib.freightReceiver;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.contrib.freightReceiver.collaboration.CollaborationUtils;
import org.matsim.contrib.freightReceiver.replanning.ReceiverOrderStrategyManagerFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This controller ensures that each receiver receives a cost (score) per order at the end of each iteration and replans its orders based on the cost of the previous iteration and past iterations.
 *
 * @author wlbean
 *
 */
class ReceiverControlerListener implements ScoringListener,
        ReplanningListener, BeforeMobsimListener, ShutdownListener {

    final private static Logger LOG = LogManager.getLogger(ReceiverControlerListener.class);
    private final ReceiverOrderStrategyManagerFactory stratManFac;
    private final ReceiverScoringFunctionFactory scorFuncFac;
    private ReceiverTracker tracker;
    @Inject
    EventsManager eMan;
    @Inject
    Scenario sc;

    @Inject
    private ReceiverControlerListener(ReceiverOrderStrategyManagerFactory stratManFac, ReceiverScoringFunctionFactory scorFuncFac) {
        this.stratManFac = stratManFac;
        this.scorFuncFac = scorFuncFac;
    }

    @Override
    public void notifyReplanning(final ReplanningEvent event) {

        if (stratManFac == null) {
            return;
        }

        GenericStrategyManager<ReceiverPlan, Receiver> stratMan = stratManFac.createReceiverStrategyManager();

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
            stratMan.run(receiverControlCollection, event.getIteration(), event.getReplanningContext());

            /* Run replanning for grand coalition receivers.*/
            stratMan.run(receiverCollection, event.getIteration(), event.getReplanningContext());
        }
    }


    /*
     * Determines the order cost at the end of each iteration.
     */

    @Override
    public void notifyScoring(ScoringEvent event) {
        if (event.getIteration() == 0) {
            this.tracker.scoreSelectedPlans();
        }

//		if ((event.getIteration()+1) % ReceiverUtils.getReplanInterval( sc ) == 0) {
        if ((event.getIteration() + 1) % ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).getReceiverReplanningInterval() == 0 && event.getIteration() > 0) {
            // this is called in the iteration after the replanning iteration.
            this.tracker.scoreSelectedPlans();
        } else {
            // this is called in all other iterations.  why?
            for (Receiver receiver : ReceiverUtils.getReceivers(sc).getReceivers().values()) {
                double score = (double) receiver.getAttributes().getAttribute(ReceiverUtils.ATTR_RECEIVER_SCORE);
//				double score = (double) receiver.getSelectedPlan().getScore();
                receiver.getSelectedPlan().setScore(score);
            }
        }
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        tracker = new ReceiverTracker(scorFuncFac, sc);
//		eMan.addHandler(tracker);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        /* Method to check the status of time windows. */
        try {
            linkReceiverTimewindowToCarrierTourPosition();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot write receiver output, linking them to tour positions.");
        }

    }

    private void linkReceiverTimewindowToCarrierTourPosition() throws IOException {
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

		try (BufferedWriter bw = IOUtils.getBufferedWriter(this.sc.getConfig().controler().getOutputDirectory() + "output_receiverInTourPlacement.csv.gz")) {
			bw.write("receiverId,twStart,twEnd,twDuration,positionInTour,product,deliveryStart,deliveryEnd");
			bw.newLine();

			for (Carrier carrier : FreightUtils.getCarriers(this.sc).getCarriers().values()) {
				Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
				for (ScheduledTour tour : scheduledTours) {
					for (int i = 0; i < tour.getTour().getTourElements().size(); i++) {
						Tour.TourElement element = tour.getTour().getTourElements().get(i);
						if (element instanceof Tour.ShipmentBasedActivity) {
							Tour.ShipmentBasedActivity act = (Tour.ShipmentBasedActivity) element;

							String shipmentId = act.getShipment().getId().toString();
							if (act.getActivityType().equalsIgnoreCase("delivery")) {
								Id<Link> linkId = act.getShipment().getTo();
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


}
