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

package playground.jbischoff.taxi.demand;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;


/**
 * @author jbischoff
 */

public class MielecDemandExtender
{

    private String inputPlansFile = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\20.plans.xml.gz";
    private String inputNetFile = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\network.xml";

    private String inputTaxiDemand = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\taxiCustomers_05_pc.txt";
    private String outputTaxiDemandDir = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\increaseddemand\\taxidemand\\";

    private TreeSet<Id<Person>> customerIds = new TreeSet<>();
    private HashSet<Id<Person>> agentIds;
    private int MAXIMUMDEMANDINPERCENT = 50;


    /**
     * @param args
     */

    public static void main(String[] args)
    {

        MielecDemandExtender mde = new MielecDemandExtender();
        mde.readPlans();
        mde.readCustomers();
        mde.fillCustomers();
    }


    private void readCustomers()
    {

        List<String> taxiCustomerIds;
        taxiCustomerIds = PersonCreatorWithRandomTaxiMode.readTaxiCustomerIds(inputTaxiDemand);
        for (String s : taxiCustomerIds) {
            customerIds.add(Id.create(s, Person.class));
        }
    }


    private void fillCustomers()
    {
        double d = (double)customerIds.size() / (double)agentIds.size();
        long currentPercentage = Math.round(d * 100);
        Random r = new Random();
        exportCustomers(currentPercentage);
        currentPercentage++;
        for (; currentPercentage <= MAXIMUMDEMANDINPERCENT; currentPercentage++) {
            long amountOfPlans = Math
                    .round( ( (currentPercentage / 100.) * agentIds.size()));
            System.out.println(amountOfPlans);
            for (long i = customerIds.size(); i <= amountOfPlans; i++) {
                addCustomer(r);
            }
            exportCustomers(currentPercentage);
        }
    }


    private void exportCustomers(long percentage)
    {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputTaxiDemandDir
                    + "taxiCustomers_" + percentage + ".txt")));

            TreeSet<Integer> ints = new TreeSet<Integer>();

            for (Id<Person> cid : customerIds) {
                ints.add(Integer.parseInt(cid.toString()));
            }

            for (Integer i : ints) {
                bw.append(i.toString());
                bw.newLine();
            }
            System.out
                    .println("Wrote " + percentage + " with " + customerIds.size() + " customers");
            bw.flush();
            bw.close();

        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    private void addCustomer(Random r)
    {

        do {
            Id<Person> cid = Id.create(r.nextInt(agentIds.size()), Person.class);
            if (!customerIds.contains(cid)) {
                if (agentIds.contains(cid)) {
                    customerIds.add(cid);
                    break;
                }
            }

        }
        while (true);
    }


    private void readPlans()
    {
        Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(sc.getNetwork()).readFile(inputNetFile);
        new MatsimPopulationReader(sc).readFile(inputPlansFile);
        agentIds = new HashSet<>();
        for (Entry<Id<Person>, ? extends Person> e : sc.getPopulation().getPersons().entrySet()) {
            if (e.getKey().equals(Id.create("1398", Person.class)))
                continue;
            Activity a = (Activity)e.getValue().getSelectedPlan().getPlanElements().get(0);
            Coord coorda = sc.getNetwork().getLinks().get(a.getLinkId()).getCoord();
            Activity b = (Activity)e.getValue().getSelectedPlan().getPlanElements().get(2);
            Coord coordb = sc.getNetwork().getLinks().get(b.getLinkId()).getCoord();
            if (isInCity(coorda) && isInCity(coordb)) {
                agentIds.add(e.getKey());
            }

        }
        System.out.println("found " + agentIds.size() + " within city trips");
        //		agentIds = new HashSet<Id>(sc.getPopulation().getPersons().keySet());

    }


    private boolean isInCity(Coord c)
    {
        double x = c.getX();
        double y = c.getY();

        if (x < -500)
            return false;
        if (x > 8000)
            return false;
        if (y < -8000)
            return false;
        if (y > 500)
            return false;

        return true;

    }

}
