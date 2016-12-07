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

import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimulatedAnnealingTravelDisutility implements TravelDisutility, LinkEnterEventHandler, PersonArrivalEventHandler, AfterMobsimListener, BeforeMobsimListener {


    private double tc = 0;
    private int cnt = 0;

    private HashMap<Id<Link>, LinkInfo> lis = new HashMap<>();

    private Set<LinkInfo> activeLinks = new HashSet<>();
    private int iteration;

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        LinkInfo li = this.lis.get(link.getId());
        if (li == null) {
            return 1.;
        }
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
        activeLinks.clear();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        for (LinkInfo li : this.lis.values()) {
            li.cnt = iteration;
            if (this.activeLinks.contains(li)) {
                li.tc = (li.cnt / (li.cnt + 1.0)) * li.tc + (1.0 / (li.cnt + 1.0)) * this.tc;
                li.cnt++;
            } else {
                li.tc = (li.cnt / (li.cnt + 1.0)) * li.tc;
                li.cnt++;
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        LinkInfo li = this.lis.computeIfAbsent(event.getLinkId(), k -> new LinkInfo());
        activeLinks.add(li);
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
        int cnt = 0;
        double tc = 0;
    }
}
