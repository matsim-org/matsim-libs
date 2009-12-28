/* *********************************************************************** *
 * project: org.matsim.*
 * CutNetwork.java
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

package playground.gregor.gis.networkcutter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.evacuation.base.EvacuationAreaFileReader;
import org.matsim.evacuation.base.EvacuationAreaFileWriter;
import org.matsim.evacuation.base.EvacuationAreaLink;
import org.matsim.world.World;
import org.xml.sax.SAXException;


public class CutNetwork {

	private static double max_x = 652088.;
	private static double max_y = 9894785.;
	private final static double MIN_X = 650473.;
	private final static double MIN_Y = 9892816.;
	
	private static void cutIt(final NetworkLayer net,
			final List<NetworkChangeEvent> events, final PopulationImpl pop, final HashMap<Id,EvacuationAreaLink> eal) {
		
		max_x = 652000.;
		max_y = 9894780.;
		ConcurrentLinkedQueue<Id> ealq = new ConcurrentLinkedQueue<Id>();
		for (Entry<Id, EvacuationAreaLink> e :  eal.entrySet()) {
			if (!isWithin(net.getLink(e.getKey()).getCoord())) {
				ealq.add(e.getKey());
			}
		}
		System.out.println("EAL:" + eal.size());
		while (ealq.size() > 0) {
			Id el = ealq.poll();
			eal.remove(el);
		}
		System.out.println("EAL:" + eal.size());
		max_x = 652088.;
		max_y = 9894785.;
		
		ConcurrentLinkedQueue<Plan> q = new ConcurrentLinkedQueue<Plan>();
		for (Person pers : pop.getPersons().values()) {
			Plan p = pers.getSelectedPlan();
			CoordImpl c = (CoordImpl) ((PlanImpl) p).getFirstActivity().getCoord();
			if (!isWithin(c)) {
				q.add(p);
			}
		}
		while (q.size() > 0) {
			Plan p = q.poll();
			Person pers = p.getPerson();
			pop.getPersons().remove(pers.getId());
		}
		System.out.println(pop.getPersons().size());

		
		//CHANGE EVENTS
		ConcurrentLinkedQueue<NetworkChangeEvent> eq = new ConcurrentLinkedQueue<NetworkChangeEvent>();
		for (NetworkChangeEvent e : events) {
			ConcurrentLinkedQueue<LinkImpl> lq = new ConcurrentLinkedQueue<LinkImpl>();
			for (LinkImpl l : e.getLinks()) {
				if (!eal.containsKey(l.getId())) {
					lq.add(l);
				}
			}
			while (lq.size() > 0) {
				LinkImpl l = lq.poll();
				e.removeLink(l);
			}
			if (e.getLinks().size() == 0) {
				eq.add(e);
			}
		}
		while (eq.size() > 0) {
			NetworkChangeEvent e = eq.poll();
			events.remove(e);
		}
		ConcurrentLinkedQueue<LinkImpl> lq = new ConcurrentLinkedQueue<LinkImpl>();
		for (LinkImpl l : net.getLinks().values()) {
			if (!isWithin(l.getCoord())) {
				lq.add(l);
			}
		}
		while (lq.size() > 0) {
			LinkImpl l = lq.poll();
			net.removeLink(l);
		}
		new NetworkCleaner().run(net);
		
		Iterator<? extends Person> it = pop.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((ActivityImpl)pers.getPlans().get(0).getPlanElements().get(0)).getLink().getId();

			if (net.getLink(id) == null) {
				it.remove();
			}
		}
		System.out.println(pop.getPersons().size());
		
	}
	
	private static boolean isWithin(final Coord c) {

		if (c.getX() < MIN_X || c.getX() > max_x || c.getY() < MIN_Y || c.getY() > max_y) {
			return false;
		}
		return true;
	}
	
	public static void main(final String [] args) {
		Config c = Gbl.createConfig(args);
		World w = Gbl.createWorld();
		
		NetworkFactoryImpl nf = new NetworkFactoryImpl();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		NetworkLayer net = new NetworkLayer(nf);
		new MatsimNetworkReader(net).readFile(c.network().getInputFile());
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(net);
		try {
			parser.parse(c.network().getChangeEventsInputFile());
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		net.setNetworkChangeEvents(parser.getEvents());
		
		
		w.setNetworkLayer(net);
		PopulationImpl pop = new PopulationImpl();
		try {
			new MatsimPopulationReader(pop,net).parse(c.plans().getInputFile());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
		String evacuationAreaLinksFile = c.evacuation().getEvacuationAreaFile();
		try {
			new EvacuationAreaFileReader(evacuationAreaLinks).readFile(evacuationAreaLinksFile);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(evacuationAreaLinks.size());
		cutIt(net,parser.getEvents(),pop,evacuationAreaLinks);
		System.out.println(evacuationAreaLinks.size());
		
		new NetworkWriter(net).writeFile("tmp2/network.xml");
		new NetworkChangeEventsWriter().write("tmp2/changeEvents.xml", parser.getEvents());
		new PopulationWriter(pop).writeFile("tmp2/population.xml");
		try {
			new EvacuationAreaFileWriter(evacuationAreaLinks).writeFile("tmp2/evacuationArea.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
