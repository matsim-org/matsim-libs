/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.evaluation;


import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.contrib.taxi.TaxiUtils;

import com.google.common.math.DoubleMath;


/**
 * @author jbischoff
 */
public class TaxiCustomerWaitTimeAnalyser
    implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler
{

    private Map<Id, Double> taxicalltime;
    private List<Double> totalWaitTime;
    private Map<Id, Double> linkWaitTime;
    private Map<Id, Long> linkWaitPeople;
    private Map<Id, Id> linkAg;
    private Scenario scenario;
    private List<WaitTimeLogRow> waitTimes;


    public TaxiCustomerWaitTimeAnalyser(Scenario scen)
	{
		this(scen, Double.MAX_VALUE);
	}


	public TaxiCustomerWaitTimeAnalyser(Scenario scen, double upperLimit)
    {
        this.scenario = scen;
        this.taxicalltime = new HashMap<Id, Double>();
        this.linkWaitTime = new TreeMap<Id, Double>();
        this.linkWaitPeople = new HashMap<Id, Long>();
        this.linkAg = new HashMap<Id, Id>();
        this.totalWaitTime = new ArrayList<Double>();
        this.waitTimes = new ArrayList<WaitTimeLogRow>();
        
    }


    public class WaitTimeLogRow
        implements Comparable<WaitTimeLogRow>
    {
        private Double time;
        private Id agentId;
        private Id linkId;
        private double waitTime;


        public WaitTimeLogRow(double time, Id agent, Id link, double waitTime)
        {
            this.time = time;
            this.agentId = agent;
            this.linkId = link;
            this.waitTime = waitTime;
        }


        public double getTime()
        {
            return time;
        }


        public String toString()
        {

            return (this.time + "\t" + this.agentId + "\t" + this.linkId + "\t" + this.waitTime);
        }


        @Override
        public int compareTo(WaitTimeLogRow arg0)
        {
            return this.time.compareTo(arg0.getTime());
        }

    }


    @Override
    public void reset(int iteration)
    {

    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event)
    {
        if (!this.taxicalltime.containsKey(event.getPersonId()))
            return;
        double waitingtime = event.getTime() - this.taxicalltime.get(event.getPersonId());
        this.totalWaitTime.add(waitingtime);

        double linkWait = waitingtime;
        if (this.linkWaitTime.containsKey(this.linkAg.get(event.getPersonId())))
            linkWait = linkWait + this.linkWaitTime.get(this.linkAg.get(event.getPersonId()));
        this.linkWaitTime.put(this.linkAg.get(event.getPersonId()), linkWait);

        this.waitTimes.add(new WaitTimeLogRow(event.getTime(), event.getPersonId(), this.linkAg
                .get(event.getPersonId()), waitingtime));
        this.taxicalltime.remove(event.getPersonId());
        this.linkAg.remove(event.getPersonId());

    }


    @Override
    public void handleEvent(PersonDepartureEvent event)
    {
        if (!event.getLegMode().equals(TaxiUtils.TAXI_MODE))
            return;
        this.taxicalltime.put(event.getPersonId(), event.getTime());
        this.linkAg.put(event.getPersonId(), event.getLinkId());

        long people = 0;
        ;
        if (linkWaitPeople.containsKey(event.getLinkId()))
            people = linkWaitPeople.get(event.getLinkId());
        people++;
        linkWaitPeople.put(event.getLinkId(), people);

    }


    public double calculateAverageWaitTime()
    {

    	return  DoubleMath.mean(this.totalWaitTime);

    }


    public double returnMaxWaitTime()
    {
        return Collections.max(this.totalWaitTime);

    }


    public double returnMinWaitTime()
    {
        return Collections.min(this.totalWaitTime);

    }


    public String writeCustomerWaitStats(String waitstatsFile)
    {

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(waitstatsFile)));
            bw.write("total taxi trips\taverage wait Time\tMinimum Wait Time\tMaximum Wait Time\n");
            String output = this.totalWaitTime.size() + "\t"
                    + Math.round(this.calculateAverageWaitTime()) + "\t"
                    + Math.round(this.returnMinWaitTime()) + "\t"
                    + Math.round(this.returnMaxWaitTime());
            bw.write(output);
            bw.newLine();

            bw.flush();
            bw.close();

            bw = new BufferedWriter(new FileWriter(new File(waitstatsFile + "linkWait.txt")));
            bw.append("linkId\tx\ty\tWaitTime\tTrips\n");
            for (Entry<Id, Double> e : this.linkWaitTime.entrySet()) {
                Coord coord = scenario.getNetwork().getLinks().get(e.getKey()).getCoord();
                bw.write(e.getKey() + "\t" + coord.getX() + "\t" + coord.getY() + "\t"
                        + e.getValue() + "\t" + this.linkWaitPeople.get(e.getKey()));
                bw.newLine();
            }

            bw.flush();
            bw.close();

            bw = new BufferedWriter(new FileWriter(new File(waitstatsFile + "agentWaitTimes.txt")));
            bw.append("time\tagent\tlinkId\twaitTime");
            bw.newLine();
            for (WaitTimeLogRow wtlr : this.waitTimes) {
                bw.append(wtlr.toString());
                bw.newLine();
            }

            bw.flush();
            bw.close();
            return output;
        }
        catch (IOException e) {
            System.err.println("Could not create File" + waitstatsFile);
            e.printStackTrace();
        }
        return null;

    }


    public void printTaxiCustomerWaitStatistics()
    {
        System.out.println("Number of Taxi Trips\t" + this.totalWaitTime.size());
        System.out.println("Average Waiting Time\t" + this.calculateAverageWaitTime());
        System.out.println("Maximum Waiting Time\t" + this.returnMaxWaitTime());

    }

}
