/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.wasteCollection;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

/**
 * Building a small waste collection example with all the necessary files
 * required.
 *  
 * @author jwjoubert
 */
public class SetupWasteExample {
	final private static Logger LOG = Logger.getLogger(SetupWasteExample.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SetupWasteExample.class.toString(), args);
		
		buildNetwork();
		buildWasteVehicle();
		
		Header.printFooter();
	}
	
	
	public static void buildWasteVehicle(){
		LOG.info("Building example waste vehicle(s)...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop = sc.getPopulation();
		PopulationFactory pf = pop.getFactory();
		
		List<Id<Link>> routeLinkIds = new ArrayList<>();
		routeLinkIds.add(Id.createLinkId("ab"));
		routeLinkIds.add(Id.createLinkId("bf"));
		routeLinkIds.add(Id.createLinkId("fj"));
		routeLinkIds.add(Id.createLinkId("jk"));
		routeLinkIds.add(Id.createLinkId("ko"));
		routeLinkIds.add(Id.createLinkId("op"));
		routeLinkIds.add(Id.createLinkId("pq"));
		routeLinkIds.add(Id.createLinkId("qm"));
		routeLinkIds.add(Id.createLinkId("mi"));
		routeLinkIds.add(Id.createLinkId("ih"));
		routeLinkIds.add(Id.createLinkId("hd"));
		routeLinkIds.add(Id.createLinkId("dc"));
		routeLinkIds.add(Id.createLinkId("cb"));
		routeLinkIds.add(Id.createLinkId("ba"));
		Route route = RouteUtils.createNetworkRoute(routeLinkIds, sc.getNetwork());
		route.setDistance(routeLinkIds.size()*1000.0);
		
		Leg leg = new LegImpl("waste");
		leg.setRoute(route);
		
		Activity first = pf.createActivityFromLinkId("depot", Id.createLinkId("ab"));
		first.setEndTime(0.0);
		Activity last = pf.createActivityFromLinkId("depot", Id.createLinkId("ba"));
				
		Plan plan = new PlanImpl();
		plan.addActivity(first);
		plan.addLeg(leg);
		plan.addActivity(last);
		
		Person person = pf.createPerson(Id.createPersonId("waste_01"));
		person.addPlan(plan);
		pop.addPerson(person);
		
		new PopulationWriter(pop).write("/Volumes/Nifty/workspace/data-wasteExample/population.xml");
		
		/* Object attributes: subpopulation. */
		ObjectAttributes oa = new ObjectAttributes();
		for(Person p : pop.getPersons().values()){
			oa.putAttribute(p.getId().toString(), "subpopulation", "waste");
		}
		new ObjectAttributesXmlWriter(oa).writeFile("/Volumes/Nifty/workspace/data-wasteExample/populationAttributes.xml");
	}
	
	
	
	/**
	 * Build a small grid-like scenario with two-way streets.
	 * 
	 *                               Intermediate
	 *          n-----o-----p-----q  facility
	 * 			|     |     |     |
	 * 			|     |     |     |
	 * 			j-----k-->--l-----m
	 * 			|     |     |     |
	 * 			|     ^     v     |
	 * 			f-----g--<--h-----i
	 * 			|     |     |     |
	 * Depot    |     |     |     |
	 *   a------b-----c-----d-----e 
	 * 
	 * Each link is 200 metres, and all links are required, except the lowest
	 * line, that is ab, bc, cd, de. The waste demand on each link is 10 units,
	 * so the total demand is 210 units. Internal links are one-way.
	 */
	public static void buildNetwork(){
		LOG.info("Building example waste network...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		NetworkFactory nf = network.getFactory();
		
		/* Nodes */
		Node a = nf.createNode(Id.createNodeId("a"), new Coord(0.0, 0.0)); network.addNode(a);
		Node b = nf.createNode(Id.createNodeId("b"), new Coord(200.0, 0.0)); network.addNode(b);
		Node c = nf.createNode(Id.createNodeId("c"), new Coord(400.0, 0.0)); network.addNode(c);
		Node d = nf.createNode(Id.createNodeId("d"), new Coord(600.0, 0.0)); network.addNode(d);
		Node e = nf.createNode(Id.createNodeId("e"), new Coord(800.0, 0.0)); network.addNode(e);
		Node f = nf.createNode(Id.createNodeId("f"), new Coord(200.0, 200.0)); network.addNode(f);
		Node g = nf.createNode(Id.createNodeId("g"), new Coord(400.0, 200.0)); network.addNode(g);
		Node h = nf.createNode(Id.createNodeId("h"), new Coord(600.0, 200.0)); network.addNode(h);
		Node i = nf.createNode(Id.createNodeId("i"), new Coord(800.0, 200.0)); network.addNode(i);
		Node j = nf.createNode(Id.createNodeId("j"), new Coord(200.0, 400.0)); network.addNode(j);
		Node k = nf.createNode(Id.createNodeId("k"), new Coord(400.0, 400.0)); network.addNode(k);
		Node l = nf.createNode(Id.createNodeId("l"), new Coord(600.0, 400.0)); network.addNode(l);
		Node m = nf.createNode(Id.createNodeId("m"), new Coord(800.0, 400.0)); network.addNode(m);
		Node n = nf.createNode(Id.createNodeId("n"), new Coord(200.0, 600.0)); network.addNode(n);
		Node o = nf.createNode(Id.createNodeId("o"), new Coord(400.0, 600.0)); network.addNode(o);
		Node p = nf.createNode(Id.createNodeId("p"), new Coord(600.0, 600.0)); network.addNode(p);
		Node q = nf.createNode(Id.createNodeId("q"), new Coord(800.0, 600.0)); network.addNode(q);
		
		/* Links */
		Link ab = nf.createLink(Id.createLinkId("ab"), a, b); network.addLink(ab);
		Link ba = nf.createLink(Id.createLinkId("ba"), b, a); network.addLink(ba);
		Link bc = nf.createLink(Id.createLinkId("bc"), b, c); network.addLink(bc);
		Link cb = nf.createLink(Id.createLinkId("cb"), c, b); network.addLink(cb);
		Link cd = nf.createLink(Id.createLinkId("cd"), c, d); network.addLink(cd);
		Link dc = nf.createLink(Id.createLinkId("dc"), d, c); network.addLink(dc);
		Link de = nf.createLink(Id.createLinkId("de"), d, e); network.addLink(de);
		Link ed = nf.createLink(Id.createLinkId("ed"), e, d); network.addLink(ed);
		
		Link ih = nf.createLink(Id.createLinkId("ih"), i, h); network.addLink(ih);
		Link hg = nf.createLink(Id.createLinkId("hg"), h, g); network.addLink(hg);
		Link gf = nf.createLink(Id.createLinkId("gf"), g, f); network.addLink(gf);
		
		Link jk = nf.createLink(Id.createLinkId("jk"), j, k); network.addLink(jk);
		Link kl = nf.createLink(Id.createLinkId("kl"), k, l); network.addLink(kl);
		Link lm = nf.createLink(Id.createLinkId("lm"), l, m); network.addLink(lm);
		
		Link no = nf.createLink(Id.createLinkId("no"), n, o); network.addLink(no);
		Link on = nf.createLink(Id.createLinkId("on"), o, n); network.addLink(on);
		Link op = nf.createLink(Id.createLinkId("op"), o, p); network.addLink(op);
		Link po = nf.createLink(Id.createLinkId("po"), p, o); network.addLink(po);
		Link pq = nf.createLink(Id.createLinkId("pq"), p, q); network.addLink(pq);
		Link qp = nf.createLink(Id.createLinkId("qp"), q, p); network.addLink(qp);
		
		Link bf = nf.createLink(Id.createLinkId("bf"), b, f); network.addLink(bf);
		Link fb = nf.createLink(Id.createLinkId("fb"), f, b); network.addLink(fb);
		Link fj = nf.createLink(Id.createLinkId("fj"), f, j); network.addLink(fj);
		Link jf = nf.createLink(Id.createLinkId("jf"), j, f); network.addLink(jf);
		Link jn = nf.createLink(Id.createLinkId("jn"), j, n); network.addLink(jn);
		Link nj = nf.createLink(Id.createLinkId("nj"), n, j); network.addLink(nj);

		Link cg = nf.createLink(Id.createLinkId("cg"), c, g); network.addLink(cg);
		Link gk = nf.createLink(Id.createLinkId("gk"), g, k); network.addLink(gk);
		Link ko = nf.createLink(Id.createLinkId("ko"), k, o); network.addLink(ko);

		Link pl = nf.createLink(Id.createLinkId("pl"), p, l); network.addLink(pl);
		Link lh = nf.createLink(Id.createLinkId("lh"), l, h); network.addLink(lh);
		Link hd = nf.createLink(Id.createLinkId("hd"), h, d); network.addLink(hd);
		
		Link ei = nf.createLink(Id.createLinkId("ei"), e, i); network.addLink(ei);
		Link ie = nf.createLink(Id.createLinkId("ie"), i, e); network.addLink(ie);
		Link im = nf.createLink(Id.createLinkId("im"), i, m); network.addLink(im);
		Link mi = nf.createLink(Id.createLinkId("mi"), m, i); network.addLink(mi);
		Link mq = nf.createLink(Id.createLinkId("mq"), m, q); network.addLink(mq);
		Link qm = nf.createLink(Id.createLinkId("qm"), q, m); network.addLink(qm);
		
		/* Set the free speed to 60km/h */
		for(Link link : network.getLinks().values()){
			link.setFreespeed(60.0 / 3.6);
			link.setLength(200.0);
		}
		
		Link[] demand = {ih, hg, gf, jk, kl, lm,
				no, op, pq, qp, po, on,
				bf, fj, jn, nj, jf, fb,
				ei, im, mq, qm, mi, ie};
		ObjectAttributes oa = new ObjectAttributes();
		for(Link link : demand){
			oa.putAttribute(link.getId().toString(), "waste", 10);
		}
		
		new NetworkWriter(network).write("/Volumes/Nifty/workspace/data-wasteExample/network.xml");
		new ObjectAttributesXmlWriter(oa).writeFile("/Volumes/Nifty/workspace/data-wasteExample/networkAttributes.xml");
	}

}
