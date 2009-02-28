/* *********************************************************************** *
 * project: org.matsim.*
 * FilterBerlinKutter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.ActImpl;
import org.matsim.population.LegImpl;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PlanImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.world.World;

class EventHH implements LinkEnterEventHandler {

	static Set<String> linkList = new HashSet<String>();

	public void handleEvent(final LinkEnterEvent event) {
		linkList.add(event.linkId);
	}

	public void reset(final int iteration) {
	}

}

class FilterPersons2 extends AbstractPersonAlgorithm{

	public static Set<Node> relevantFromNodes = new HashSet<Node>();
	public static Set<Node> relevantToNodes = new HashSet<Node>();

	int modulo = 1;
	int count = 0;
	public static int ptCount = 0;
	
	public Set<Link> usedlinkList = new HashSet<Link>();

	
	public FilterPersons2() {
		super();
	}

	public void addLinks(final Plan p) {
		List<?> actl = p.getActsLegs();
		for (int i= 0; i< actl.size() ; i++) {
				if (i % 2 == 0) {
					// activity
					Act a = (Act)actl.get(i);
					this.usedlinkList.add(a.getLink());
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					if(l.getMode().equals(BasicLeg.Mode.car) && l.getRoute() != null){
						
						List<Link> ll = ((CarRoute) l.getRoute()).getLinks();
						for(Link link : ll) {
							usedlinkList.add(link);
						}
					}
				}
		}

	}
	Plan copyPlanToPT(final Plan in) {
		Plan erg = new PlanImpl(in.getPerson());
		List<?> actl = in.getActsLegs();
		for (int i= 0; i< actl.size() ; i++) {
			try {
				if (i % 2 == 0) {
					// activity
					Act a = (Act)actl.get(i);
					erg.getActsLegs().add(new ActImpl(a));
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					Leg l2 = new LegImpl(BasicLeg.Mode.pt);
					l2.setDepartureTime(l.getDepartureTime());
					l2.setTravelTime(l.getTravelTime());
					l2.setArrivalTime(l.getArrivalTime());
					erg.getActsLegs().add(l2);
				}
			} catch (Exception e) {
				// copying a plan is fairly basic. if an exception occurs here, something
				// must be definitively wrong -- exit with an error
				Gbl.errorMsg(e);
			}
		}
		return erg;
	}
	@Override
	public void run(final Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		Plan plan = person.getSelectedPlan();
		Leg leg = plan.getNextLeg(plan.getFirstActivity());
		if(leg.getMode().equals(BasicLeg.Mode.car) && leg.getRoute() == null) {
			// car leg without route make all legs mode = PT
			plan.setSelected(false);
			plan = copyPlanToPT(plan);
			ptCount++;
			person.addPlan(plan);
			plan.setSelected(true);
			person.setSelectedPlan(plan);
		}
		person.removeUnselectedPlans();
		if(count > 1000000) return; //just write 1 mio plans and the ignore the rest for now
		
		try {
			if ((count % 100) == 0) ReducePopulationExe.plansWriter1.writePerson(person);
			if ((count % 10) == 0) ReducePopulationExe.plansWriter10.writePerson(person);
			if ((count % 4) == 0) ReducePopulationExe.plansWriter25.writePerson(person);
			if ((count % 2) == 0) ReducePopulationExe.plansWriter50.writePerson(person);
			ReducePopulationExe.plansWriter100.writePerson(person);
			addLinks(plan);
		} catch (Exception e) {
			e.printStackTrace();
		}
		count++;
		if(count % 10000 == 0) {
			System.out.println("");
			System.out.println("Count == " + count + " PTCount = " + ptCount + " Anteil PT: " + (ptCount/1.0*count));
		}
	}
}

public class ReducePopulationExe {
	public static NetworkLayer network;
	public static String outpopFileName = "../../tmp/studies/ivtch/Diss/input/plans";

	public static Population relevantPopulation;
	public static PopulationWriter plansWriter1;
	public static PopulationWriter plansWriter10 ;
	public static PopulationWriter plansWriter25;
	public static PopulationWriter plansWriter50 ;
	public static PopulationWriter plansWriter100 ;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final int b;
		b=324;

		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		//String netFileName = "../../tmp/studies/ivtch/ivtch-osm.xml";
		String netFileName = "../../tmp/studies/ivtch/Diss/input/ivtch-osm.xml";
		String popFileName = "../../tmp/studies/ivtch/Diss/input/plans_all_187k.xml";
		String outnetFileName = "../../tmp/studies/ivtch/Diss/input/ivtch_red100.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		World world = Gbl.getWorld();

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		world.complete();

		relevantPopulation = new PopulationImpl(PopulationImpl.USE_STREAMING);
		plansWriter1 = new PopulationWriter(relevantPopulation, outpopFileName + "1p.xml", "v4");
		plansWriter10 = new PopulationWriter(relevantPopulation, outpopFileName + "10p.xml", "v4");
		plansWriter25 = new PopulationWriter(relevantPopulation, outpopFileName + "25p.xml", "v4");
		plansWriter50 = new PopulationWriter(relevantPopulation, outpopFileName + "50p.xml", "v4");
		plansWriter100 = new PopulationWriter(relevantPopulation, outpopFileName + "100p.xml", "v4");

		Population population = new PopulationImpl(PopulationImpl.USE_STREAMING);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population);
		FilterPersons2 filter = new FilterPersons2();
		population.addAlgorithm(filter);
		plansReader.readFile(popFileName);

		System.out.println("write # persons: " );
		relevantPopulation.printPlansCount();
		population.runAlgorithms();

		plansWriter1.writeEndPlans();
		plansWriter10.writeEndPlans();
		plansWriter25.writeEndPlans();
		plansWriter50.writeEndPlans();
		plansWriter100.writeEndPlans();

		List<Link> nolinkList = new LinkedList<Link>();
		for(Link link : network.getLinks().values()) if(!filter.usedlinkList.contains(link)) nolinkList.add(link);

		for(Link link : nolinkList)network.removeLink(link);
		
		new NetworkWriter(network, outnetFileName).write();
		
	}

}
