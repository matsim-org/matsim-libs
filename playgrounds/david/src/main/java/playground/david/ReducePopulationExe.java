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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

class EventHH implements LinkEnterEventHandler {

	static Set<String> linkList = new HashSet<String>();

	public void handleEvent(final LinkEnterEvent event) {
		linkList.add(event.getLinkId().toString());
	}

	public void reset(final int iteration) {
	}

}

class FilterPersons2 extends AbstractPersonAlgorithm{

	public static Set<NodeImpl> relevantFromNodes = new HashSet<NodeImpl>();
	public static Set<NodeImpl> relevantToNodes = new HashSet<NodeImpl>();

	int modulo = 1;
	int count = 0;
	public static int ptCount = 0;

	public Set<Id> usedlinkList = new HashSet<Id>();


	public FilterPersons2() {
		super();
	}

	public void addLinks(final Plan p) {
		List<?> actl = p.getPlanElements();
		for (int i= 0; i< actl.size() ; i++) {
				if (i % 2 == 0) {
					// activity
					ActivityImpl a = (ActivityImpl)actl.get(i);
					this.usedlinkList.add(a.getLinkId());
				} else {
					// Leg
					LegImpl l = (LegImpl) actl.get(i);
					if(l.getMode().equals(TransportMode.car) && l.getRoute() != null){

						List<Id> ll = ((NetworkRouteWRefs) l.getRoute()).getLinkIds();
						for(Id linkId : ll) {
							usedlinkList.add(linkId);
						}
					}
				}
		}

	}
	PlanImpl copyPlanToPT(final Plan in) {
		PlanImpl erg = new PlanImpl(in.getPerson());
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
					LegImpl l = (LegImpl) actl.get(i);
					LegImpl l2 = new LegImpl(TransportMode.pt);
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
		Leg leg = ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity());
		if(leg.getMode().equals(TransportMode.car) && leg.getRoute() == null) {
			// car leg without route make all legs mode = PT
			plan.setSelected(false);
			plan = copyPlanToPT(plan);
			ptCount++;
			person.addPlan(plan);
			plan.setSelected(true);
			((PersonImpl) person).setSelectedPlan(plan);
		}
		((PersonImpl) person).removeUnselectedPlans();
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

		Config config = Gbl.createConfig(args);
		ScenarioImpl scenario = new ScenarioImpl(config);

		network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFileName);

		relevantPopulation = new ScenarioImpl().getPopulation();
		relevantPopulation.setIsStreaming(true);
		plansWriter1 = new PopulationWriter(relevantPopulation, network);
		plansWriter10 = new PopulationWriter(relevantPopulation, network);
		plansWriter25 = new PopulationWriter(relevantPopulation, network);
		plansWriter50 = new PopulationWriter(relevantPopulation, network);
		plansWriter100 = new PopulationWriter(relevantPopulation, network);
		plansWriter1.startStreaming(outpopFileName + "1p.xml");
		plansWriter10.startStreaming(outpopFileName + "10p.xml");
		plansWriter25.startStreaming(outpopFileName + "25p.xml");
		plansWriter50.startStreaming(outpopFileName + "50p.xml");
		plansWriter100.startStreaming(outpopFileName + "100p.xml");

		PopulationImpl population = scenario.getPopulation();
		population.setIsStreaming(true);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
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

		List<LinkImpl> nolinkList = new LinkedList<LinkImpl>();
		for(LinkImpl link : network.getLinks().values()) if(!filter.usedlinkList.contains(link.getId())) nolinkList.add(link);

		for(LinkImpl link : nolinkList)network.removeLink(link);

		new NetworkWriter(network).writeFile(outnetFileName);

	}

}
