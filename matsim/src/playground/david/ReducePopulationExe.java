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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

class EventHH implements BasicLinkEnterEventHandler {

	static Set<String> linkList = new HashSet<String>();

	public void handleEvent(final BasicLinkEnterEvent event) {
		linkList.add(event.getLinkId().toString());
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
		List<?> actl = p.getPlanElements();
		for (int i= 0; i< actl.size() ; i++) {
				if (i % 2 == 0) {
					// activity
					ActivityImpl a = (ActivityImpl)actl.get(i);
					this.usedlinkList.add(a.getLink());
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					if(l.getMode().equals(TransportMode.car) && l.getRoute() != null){
						
						List<Link> ll = ((NetworkRoute) l.getRoute()).getLinks();
						for(Link link : ll) {
							usedlinkList.add(link);
						}
					}
				}
		}

	}
	Plan copyPlanToPT(final Plan in) {
		Plan erg = new PlanImpl(in.getPerson());
		List ergPEs = erg.getPlanElements();
		List<?> actl = in.getPlanElements();
		for (int i= 0; i< actl.size() ; i++) {
			try {
				if (i % 2 == 0) {
					// activity
					ActivityImpl a = (ActivityImpl)actl.get(i);
					ergPEs.add(new ActivityImpl(a));
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					Leg l2 = new LegImpl(TransportMode.pt);
					l2.setDepartureTime(l.getDepartureTime());
					l2.setTravelTime(l.getTravelTime());
					l2.setArrivalTime(l.getArrivalTime());
					ergPEs.add(l2);
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
		if(leg.getMode().equals(TransportMode.car) && leg.getRoute() == null) {
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

	public static PopulationImpl relevantPopulation;
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

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		relevantPopulation = new PopulationImpl();
		relevantPopulation.setIsStreaming(true);
		plansWriter1 = new PopulationWriter(relevantPopulation, outpopFileName + "1p.xml", "v4");
		plansWriter10 = new PopulationWriter(relevantPopulation, outpopFileName + "10p.xml", "v4");
		plansWriter25 = new PopulationWriter(relevantPopulation, outpopFileName + "25p.xml", "v4");
		plansWriter50 = new PopulationWriter(relevantPopulation, outpopFileName + "50p.xml", "v4");
		plansWriter100 = new PopulationWriter(relevantPopulation, outpopFileName + "100p.xml", "v4");

		PopulationImpl population = new PopulationImpl();
		population.setIsStreaming(true);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, network);
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
