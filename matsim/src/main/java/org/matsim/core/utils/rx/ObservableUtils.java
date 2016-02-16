/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ObservableUtils.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.utils.rx;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.SafeSubscriber;
import rx.subscriptions.Subscriptions;

public class ObservableUtils {

    private static class ControlerListenerAdapter implements StartupListener, IterationStartsListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, ScoringListener, IterationEndsListener, ShutdownListener {

        private final Subscriber<ControlerEvent> subscriber;

        ControlerListenerAdapter(Subscriber<ControlerEvent> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyAfterMobsim(AfterMobsimEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyBeforeMobsim(BeforeMobsimEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyReplanning(ReplanningEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyScoring(ScoringEvent event) {
            subscriber.onNext(event);
        }

        @Override
        public void notifyShutdown(ShutdownEvent event) {
            subscriber.onNext(event);
            if (event.isUnexpected()) {
                subscriber.onError(new RuntimeException());
            } else {
                subscriber.onCompleted();
            }
        }

    }

    public static Observable<ControlerEvent> fromControlerListenerManager(final ControlerListenerManager controlerListenerManager) {
        return Observable.create(new Observable.OnSubscribe<ControlerEvent>() {
            @Override
            public void call(Subscriber<? super ControlerEvent> subscriber) {
                final ControlerListener controlerListener = new ControlerListenerAdapter(new SafeSubscriber<>(subscriber));
                controlerListenerManager.addControlerListener(controlerListener);
            }
        });
    }

    /**
     * Wraps an EventsManager as an Observable.
     * The resulting Observable never finishes.
     */
    public static Observable<Event> fromEventsManager(final EventsManager eventsManager) {
        return Observable.create(new Observable.OnSubscribe<Event>() {

            @Override
            public void call(final Subscriber<? super Event> subscriber) {
                final BasicEventHandler handler = new BasicEventHandler() {
                    @Override
                    public void handleEvent(Event event) {
                        subscriber.onNext(event);
                    }

                    @Override
                    public void reset(int iteration) {

                    }
                };
                eventsManager.addHandler(handler);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        eventsManager.removeHandler(handler);
                    }
                }));
            }
        }).onBackpressureBlock();
    }

    public static Observable<PersonExperiencedLeg> fromEventsToLegs(final EventsToLegs eventsToLegs) {
        return Observable.create(new Observable.OnSubscribe<PersonExperiencedLeg>() {
            @Override
            public void call(final Subscriber<? super PersonExperiencedLeg> subscriber) {
                final EventsToLegs.LegHandler handler = new EventsToLegs.LegHandler() {
                    @Override
                    public void handleLeg(PersonExperiencedLeg leg) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(leg);
                    }
                };
                eventsToLegs.addLegHandler(handler);
            }
        }).onBackpressureBlock();
    }

    public static Observable<PersonExperiencedActivity> fromEventsToActivities(final EventsToActivities eventsToActivities) {
        return Observable.create(new Observable.OnSubscribe<PersonExperiencedActivity>() {
            @Override
            public void call(final Subscriber<? super PersonExperiencedActivity> subscriber) {
                final EventsToActivities.ActivityHandler handler = new EventsToActivities.ActivityHandler() {
                    @Override
                    public void handleActivity(PersonExperiencedActivity activity) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(activity);
                    }
                };
                eventsToActivities.addActivityHandler(handler);
            }
        }).onBackpressureBlock();
    }



}
