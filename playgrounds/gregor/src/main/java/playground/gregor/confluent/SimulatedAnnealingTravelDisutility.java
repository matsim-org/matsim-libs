package playground.gregor.confluent;/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class SimulatedAnnealingTravelDisutility implements TravelDisutility, LinkEnterEventHandler, PersonArrivalEventHandler, AfterMobsimListener, BeforeMobsimListener {


    private static final Logger log = Logger.getLogger(SimulatedAnnealingTravelDisutility.class);

    private double tc = 0;
    private int cnt = 0;

    private Configuration bestConfiguration;
    private Configuration randomConfiguration;

    private int iteration;

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        if (this.randomConfiguration == null) {
            return 1;
        }

        LinkInfo li = this.randomConfiguration.linkInfoHashMap.get(link.getId());

        return li.tc;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 1.;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        tc = (cnt / (cnt + 1.0) * tc) + (1.0 / (cnt + 1.0)) * event.getTime();
        cnt++;
    }

    @Override
    public void reset(int iteration) {
        this.iteration = iteration;
        tc = 0;
        cnt = 0;
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        if (randomConfiguration == null) {
            initialize(event.getServices().getScenario().getNetwork());
        }

        if (this.iteration == event.getServices().getConfig().controler().getLastIteration() - 1) {
            log.info("Kept current solution");
            for (Map.Entry<Id<Link>, LinkInfo> entry : bestConfiguration.linkInfoHashMap.entrySet()) {
                randomConfiguration.linkInfoHashMap.get(entry.getKey()).tc = entry.getValue().tc;
            }
            randomConfiguration.score = bestConfiguration.score;
            return;
        }

        double itrs = event.getServices().getConfig().controler().getLastIteration();
        double temperature = itrs / ((double) this.iteration + 1.0);
        randomConfiguration.score = -6 * (tc - 3 * 3600) / 3600;

        boolean sw;
        if (randomConfiguration.score > bestConfiguration.score) {
            sw = true;
        } else {
            double gamma = 6;

            double exp = Math.exp(-gamma * (bestConfiguration.score - randomConfiguration.score) / temperature);
            sw = MatsimRandom.getRandom().nextDouble() < exp;
        }


        if (sw) { // as of now, 0.01 is hardcoded (proba to change when both
            log.info("Switched to random solution");
            for (Map.Entry<Id<Link>, LinkInfo> entry : randomConfiguration.linkInfoHashMap.entrySet()) {
                bestConfiguration.linkInfoHashMap.get(entry.getKey()).tc = entry.getValue().tc;
            }
            bestConfiguration.score = randomConfiguration.score;

        } else {
            // scores are the same)
            log.info("Kept current solution");
            for (Map.Entry<Id<Link>, LinkInfo> entry : bestConfiguration.linkInfoHashMap.entrySet()) {
                randomConfiguration.linkInfoHashMap.get(entry.getKey()).tc = entry.getValue().tc;
            }
            randomConfiguration.score = bestConfiguration.score;

        }


        for (LinkInfo li : randomConfiguration.linkInfoHashMap.values()) {
            if (MatsimRandom.getRandom().nextDouble() < 0.01) {
                li.tc = MatsimRandom.getRandom().nextDouble() * 1000;
            }
        }

    }

    private void initialize(Network network) {
        this.randomConfiguration = new Configuration();
        this.bestConfiguration = new Configuration();
        for (Link l : network.getLinks().values()) {
            LinkInfo li = new LinkInfo();
            li.tc = MatsimRandom.getRandom().nextDouble();
            this.randomConfiguration.linkInfoHashMap.put(l.getId(), li);
            LinkInfo li2 = new LinkInfo();
            li2.tc = MatsimRandom.getRandom().nextDouble();
            this.bestConfiguration.linkInfoHashMap.put(l.getId(), li2);
            this.bestConfiguration.score = Double.NEGATIVE_INFINITY;
        }

    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (Person p : event.getServices().getScenario().getPopulation().getPersons().values()) {
            Customizable selected = p.getSelectedPlan();
            p.getPlans().removeIf(plan -> plan != selected);
        }
    }


    private static final class Configuration {
        Map<Id<Link>, LinkInfo> linkInfoHashMap = new HashMap<>();
        double score = 0;
    }


    private static final class LinkInfo {
        double tc = 0;
    }
}
