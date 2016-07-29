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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;


/**
 * @author jbischoff
 */
public class TravelDistanceTimeEvaluator
    implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler,
    PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler
{

    private Map<Id, Double> taxiTravelDistance;
    private Map<Id, Double> taxiTravelDistancesWithPassenger;
    private Network network;
    private Map<Id, Double> lastDepartureWithPassenger;
    private Map<Id, Double> taxiTravelDurationwithPassenger;
    private List<Id> isOccupied;
    private Map<Id, Double> lastDeparture;
    private Map<Id, Double> taxiTravelDuration;
    private Map<Id, Double> startTimes;
    private Map<Id, Double> endTimes;


    public TravelDistanceTimeEvaluator(Network network, double timeLimit)
    {
        this.taxiTravelDistance = new TreeMap<Id, Double>();
        this.taxiTravelDistancesWithPassenger = new HashMap<Id, Double>();
        this.taxiTravelDurationwithPassenger = new HashMap<Id, Double>();
        this.lastDepartureWithPassenger = new HashMap<Id, Double>();
        this.lastDeparture = new HashMap<Id, Double>();
        this.taxiTravelDuration = new HashMap<Id, Double>();
        this.isOccupied = new ArrayList<Id>();
        this.network = network;
        this.startTimes = new HashMap<Id, Double>();
        this.endTimes = new HashMap<Id, Double>();

    }


    public void addAgent(Id agentId)
    {
        this.taxiTravelDistance.put(agentId, 0.0);
    }


    @Override
    public void reset(int iteration)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void handleEvent(LinkLeaveEvent event)
    {
        if (!isMonitoredVehicle(event.getVehicleId()))
            return;
        double distance = this.taxiTravelDistance.get(event.getVehicleId());
        distance = distance + this.network.getLinks().get(event.getLinkId()).getLength();
        this.taxiTravelDistance.put(event.getVehicleId(), distance);
        if (this.isOccupied.contains(event.getVehicleId())) {
            double distanceWithPax = 0.;
            if (this.taxiTravelDistancesWithPassenger.containsKey(event.getVehicleId()))
                distanceWithPax = this.taxiTravelDistancesWithPassenger.get(event.getVehicleId());
            distanceWithPax = distanceWithPax
                    + this.network.getLinks().get(event.getLinkId()).getLength();
            this.taxiTravelDistancesWithPassenger.put(event.getVehicleId(), distanceWithPax);

        }
    }


    private boolean isMonitoredVehicle(Id agentId)
    {
//        return (this.taxiTravelDistance.containsKey(agentId));
    	if (agentId.toString().startsWith("rt")){
    		if (!this.taxiTravelDistance.containsKey(agentId)  				){
    			this.taxiTravelDistance.put(agentId, 0.0);
    			
    		}
    		return true;
    	}
    		return false;

    }


    public void printTravelDistanceStatistics()
    {
        double tkm = 0.;
        double tpkm = 0.;
        //		System.out.println("Agent ID\tdistanceTravelled\tdistanceTravelledWithPax\tOccupanceOverDistance\tTravelTime\tTravelTimeWithPax\tOccupanceOverTime");
        for (Entry<Id, Double> e : this.taxiTravelDistance.entrySet()) {
            double relativeOccupanceDist = tryToGetOrReturnZero(
                    this.taxiTravelDistancesWithPassenger, e.getKey()) / e.getValue();
            tpkm += tryToGetOrReturnZero(this.taxiTravelDistancesWithPassenger, e.getKey());
            double relativeOccpanceTime = tryToGetOrReturnZero(
                    this.taxiTravelDurationwithPassenger, e.getKey())
                    / (tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey()) + 0.01);
            tkm += e.getValue();
            //			System.out.println(e.getKey()+"\t"+(e.getValue()/1000)+"\t"+(tryToGetOrReturnZero(this.taxiTravelDistancesWithPassenger, e.getKey())/1000)+"\t"+relativeOccupanceDist+"\t"+tryToGetOrReturnZero( this.taxiTravelDuration, e.getKey())+"\t"+tryToGetOrReturnZero(this.taxiTravelDurationwithPassenger, e.getKey())+"\t"+relativeOccpanceTime);
        }
        tkm = tkm / 1000;
        tpkm = tpkm / 1000;

        System.out.println("Average Taxi km travelled:" + tkm / this.taxiTravelDistance.size());
        System.out.println("Average Taxi pkm travelled:" + tpkm / this.taxiTravelDistance.size());

    }


    public String writeTravelDistanceStatsToFiles(String outputFolder)
    {

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFolder+"/taxiDistanceStats.txt")));
            double tkm = 0.;
            double tpkm = 0.;
            double s = 0.;
            double ps = 0.;
            double onlineTimes = 0.;
            bw.write("Agent ID\tdistanceTravelled\tdistanceTravelledWithPax\tOccupanceOverDistance");
            for (Entry<Id, Double> e : this.taxiTravelDistance.entrySet()) {
                tpkm += tryToGetOrReturnZero(taxiTravelDistancesWithPassenger, e.getKey());
                tkm += e.getValue();
                s += tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey());
                ps += tryToGetOrReturnZero(this.taxiTravelDurationwithPassenger, e.getKey());

                bw.newLine();
                double relativeOccupanceDist = tryToGetOrReturnZero(
                        taxiTravelDistancesWithPassenger, e.getKey()) / e.getValue();
//                double relativeOccpanceTime = tryToGetOrReturnZero(
//                        this.taxiTravelDurationwithPassenger, e.getKey())
//                        / tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey());
                double startTime = 0.;
                double endTime = 0.;

                if (this.startTimes.containsKey(e.getKey()))
                    startTime = this.startTimes.get(e.getKey());
                if (this.endTimes.containsKey(e.getKey()))
                    endTime = this.endTimes.get(e.getKey());
                double onlineTime = endTime - startTime;
                onlineTimes += onlineTime;
                bw.write(e.getKey()
                        + "\t"
                        + (e.getValue() / 1000)
                        + "\t"
                        + (tryToGetOrReturnZero(this.taxiTravelDistancesWithPassenger, e.getKey()) / 1000)
                        + "\t" + relativeOccupanceDist);
//                		+ "\t"
//                        + tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey()) + "\t"
//                        + tryToGetOrReturnZero(this.taxiTravelDurationwithPassenger, e.getKey())
//                        + "\t" + relativeOccpanceTime + "\t" + startTime + "\t" + endTime + "\t"
//                        + onlineTime);

            }
            tkm = tkm / 1000;
            tkm = tkm / this.taxiTravelDistance.size();
            tpkm = tpkm / 1000;
            tpkm = tpkm / this.taxiTravelDistance.size();
            s /= this.taxiTravelDistance.size();
            ps /= this.taxiTravelDistance.size();

            bw.newLine();
            String avs = "average\t" + Math.round(tkm) + "\t" + Math.round(tpkm) + "\t"
                    + (tpkm / tkm) + "\t" + s + "\t" + ps + "\t" + (ps / s) + "\t-" + "\t-"
                    + onlineTimes / this.endTimes.size();
            bw.write(avs);

            bw.flush();
            bw.close();
            return avs;
        }
        catch (IOException e) {
            System.err.println("Could not create File in " + outputFolder);
            e.printStackTrace();
        }
        return null;
    }


    private Double tryToGetOrReturnZero(Map<Id, Double> map, Id id)
    {
        Double ret = 0.;
        if (map.containsKey(id))
            ret = map.get(id);

        return ret;

    }


    @Override
    public void handleEvent(PersonLeavesVehicleEvent event)
    {
        if (isMonitoredVehicle(event.getPersonId()))
            handleTaxiDriverLeavesEvent(event);
        if (event.getPersonId().equals(event.getVehicleId()))
            return;
        double travelTimeWithPax = event.getTime()
                - this.lastDepartureWithPassenger.get(event.getVehicleId());
        double totalTravelTimeWithPax = 0.;
        if (this.taxiTravelDurationwithPassenger.containsKey(event.getVehicleId()))
            totalTravelTimeWithPax = this.taxiTravelDurationwithPassenger.get(event.getVehicleId());
        totalTravelTimeWithPax = totalTravelTimeWithPax + travelTimeWithPax;
        this.taxiTravelDurationwithPassenger.put(event.getVehicleId(), totalTravelTimeWithPax);
        this.lastDepartureWithPassenger.remove(event.getVehicleId());
        this.isOccupied.remove(event.getVehicleId());
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event)
    {
        if (isMonitoredVehicle(event.getPersonId()))
            handleTaxiDriverEntersEvent(event);

        if (event.getPersonId().equals(event.getVehicleId()))
            return;
        this.lastDepartureWithPassenger.put(event.getVehicleId(), event.getTime());
        this.isOccupied.add(event.getVehicleId());
    }


    private void handleTaxiDriverLeavesEvent(PersonLeavesVehicleEvent event)
    {
        double travelTime = 0.;
        if (this.lastDeparture.containsKey(event.getPersonId()))
            travelTime = event.getTime() - this.lastDeparture.get(event.getPersonId());
        double totalTravelTime = 0.;
        if (this.taxiTravelDuration.containsKey(event.getPersonId()))
            totalTravelTime = this.taxiTravelDuration.get(event.getPersonId());
        totalTravelTime = totalTravelTime + travelTime;
        this.taxiTravelDuration.put(event.getPersonId(), totalTravelTime);

        this.lastDeparture.remove(event.getPersonId());
    }


    private void handleTaxiDriverEntersEvent(PersonEntersVehicleEvent event)
    {
        this.lastDeparture.put(event.getPersonId(), event.getTime());
    }


    @Override
    public void handleEvent(ActivityEndEvent event)
    {
        if (event.getActType().startsWith("Before schedule:"))
            handleBeforeSchedule(event);

    }


    private void handleBeforeSchedule(ActivityEndEvent event)
    {
        this.startTimes.put(event.getPersonId(), event.getTime());
    }


    @Override
    public void handleEvent(ActivityStartEvent event)
    {
        if (event.getActType().startsWith("After schedule:"))
            handleAfterSchedule(event);

    }


    private void handleAfterSchedule(ActivityStartEvent event)
    {
        this.endTimes.put(event.getPersonId(), event.getTime());
    }
}
