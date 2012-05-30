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
package playground.droeder.P2.setUpScenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author droeder
 *
 */
public class CreatePopulation {
	
	private static final String DIR = "D:/VSP/net/ils/roeder/6x11/";
	private static final String NET = DIR + "network2.xml"; 
	private static final String POP = DIR + "all2all.xml";
		
	public static void main(String[] args){
		List<Relation> relations = new ArrayList<Relation>();
		
		relations.add(new Relation(new IdImpl("A"), new IdImpl("B"), TransportMode.pt, 4000));
		relations.add(new Relation(new IdImpl("B"), new IdImpl("A"), TransportMode.pt, 4000));
		
		relations.add(new Relation(new IdImpl("A"), new IdImpl("C"), TransportMode.pt, 2000));
		relations.add(new Relation(new IdImpl("C"), new IdImpl("A"), TransportMode.pt, 2000));
		
		relations.add(new Relation(new IdImpl("B"), new IdImpl("C"), TransportMode.pt, 2000));
		relations.add(new Relation(new IdImpl("C"), new IdImpl("B"), TransportMode.pt, 2000));
		
		createPopulation(relations, NET, POP);
	}
	
	public static void createPopulation(List<Relation> relations, String netFile, String outfile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(netFile);
		Network net = sc.getNetwork();
		Population p = sc.getPopulation();
		PopulationFactory pFac = p.getFactory();
		
		Link o, d;
		Coord origin, destination;
		double start, interval;
		Leg l;
		Activity home, work;
		Person per;
		Plan plan;
		
		for(Relation r: relations){
			o = net.getLinks().get(r.getOriginLinkId());
			d = net.getLinks().get(r.getDestinationLinkId());
			origin = o.getToNode().getCoord();
			destination = d.getToNode().getCoord();
			start = r.getStart();
			interval = (r.getEnd()-start)/r.getNrAgents();
			
			
			for(int i = 0; i< r.getNrAgents(); i++){
				per = pFac.createPerson(sc.createId(r.getOriginLinkId() + "_" + r.getDestinationLinkId() + "_" + i));
				plan =  pFac.createPlan();
				
				home = pFac.createActivityFromCoord("home", origin);
				home.setEndTime(start);
				((ActivityImpl) home).setLinkId(o.getId());
				plan.addActivity(home);
				
				l = pFac.createLeg(TransportMode.pt);
				l.setDepartureTime(start);
				plan.addLeg(l);
				
				work = pFac.createActivityFromCoord("work", destination);
				((ActivityImpl) work).setLinkId(d.getId());
				plan.addActivity(work);
				
				per.addPlan(plan);
				p.addPerson(per);
				
				start+=interval;
			}
		}
		
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(outfile);
	}

}

class Relation{
	
	private Id from;
	private Id to;
	private String mode;
	private double nr;
	private double start = 8 * 3600;
	private double end = 10 * 3600;

	public Relation(Id fromLink, Id toLink, String mode, double nrOfAgents){
		this.from = fromLink;
		this.to = toLink;
		this.mode = mode;
		this.nr = nrOfAgents;
	}

	/**
	 * @return the from
	 */
	public Id getOriginLinkId() {
		return from;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(Id from) {
		this.from = from;
	}

	/**
	 * @return the to
	 */
	public Id getDestinationLinkId() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(Id to) {
		this.to = to;
	}

	/**
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * @return the nr
	 */
	public double getNrAgents() {
		return nr;
	}

	/**
	 * @param nr the nr to set
	 */
	public void setNr(double nr) {
		this.nr = nr;
	}

	/**
	 * @return the start
	 */
	public double getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(double start) {
		this.start = start;
	}

	/**
	 * @return the end
	 */
	public double getEnd() {
		return end;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(double end) {
		this.end = end;
	}
	
	
	
}
