/* *********************************************************************** *
 * project: org.matsim.*
 * PopProviderServer.java
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

package playground.david.otfvis.prefuse;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.core.network.NetworkLayer;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;

public class PopProviderServer implements PopulationProvider {

	public static class QueryImpl implements OTFQuery {
		public void draw(OTFDrawer drawer) {		}
		public Type getType() {			return Type.OTHER;		}
		public boolean isAlive() {			return false;		}
		public OTFQuery query(QueueNetwork net, Population population, EventsManager events,				OTFServerQuad2 quad) {	return this;	}
		public void remove() {		}
		public void setId(String id) {		}
	}

	public static class QueryIdSet extends QueryImpl {
		private SortedSet<Integer> idSet;
		
		@Override
		public OTFQuery query(QueueNetwork net, Population population, EventsManager events,
				OTFServerQuad2 quad) {
			int max = population.getPersons().size();
			Set<Id> ids = population.getPersons().keySet();
			idSet = new TreeSet<Integer>();
			for(Id id : ids){
				int i = Integer.parseInt(id.toString());
				idSet.add(i);
			}
			return this;
		}
	}

	public static class QueryPerson  extends QueryImpl {
		private Person person;
		int id;
		
		public QueryPerson(int id){
			this.id = id;
		}
		@Override
		public OTFQuery query(QueueNetwork net, Population population, EventsManager events,
				OTFServerQuad2 quad) {
			person  = population.getPersons().get(new IdImpl(id));
			return this;
		}
	}

	public static class QueryNet  extends QueryImpl {
		private NetworkLayer net;
		@Override
		public OTFQuery query(QueueNetwork net, Population population, EventsManager events,
				OTFServerQuad2 quad) {
			this.net = (NetworkLayer) net.getNetworkLayer();
			return this;
		}
	}

	public static class QueryIds  extends QueryImpl {
		private Set<Id> iId =null;
		public boolean fromNode=true;
		public QueryIds(boolean fromNode){this.fromNode = fromNode;}
		@Override
		public OTFQuery query(QueueNetwork net, Population population, EventsManager events,
				OTFServerQuad2 quad) {
			this.iId = fromNode ? net.getNetworkLayer().getNodes().keySet() : net.getNetworkLayer().getLinks().keySet();
			this.iId = new HashSet(this.iId);

			return this;
		}
	}

	public static class QueryOb  extends QueryImpl {
		public Id iId;
		public Object ob;
		public boolean fromNode=true;
		public QueryOb(Id id, boolean fromNode){this.iId = id;  this.fromNode = fromNode;}
		@Override
		public OTFQuery query(QueueNetwork net, Population population, EventsManager events,
				OTFServerQuad2 quad) {
			this.ob = fromNode ? net.getNetworkLayer().getNodes().get(iId) : net.getNetworkLayer().getLinks().get(iId);
			//System.out.println(ob.toString());
			return this;
		}
	}

	OTFClientQuad clientQ;
	NetworkLayer netti = null;
	
	
	public PopProviderServer(final OTFClientQuad clientQ) {
		this.clientQ = clientQ;
		
		if(netti == null) {
			Set<Id> nodeIds = ((QueryIds)(clientQ.doQuery(new QueryIds(true)))).iId;
			for(Id id : nodeIds) {
				clientQ.doQuery(new QueryOb(id, true));
			}
//			Set<Id> linkIds = ((QueryIds)(clientQ.doQuery(new QueryIds(false)))).iId;
//			for(Id idl : linkIds) {
//				clientQ.doQuery(new QueryOb(idl, false));
//			};
			netti = ((QueryNet)(clientQ.doQuery(new QueryNet()))).net;

			
//			Runnable loader = new Runnable() {
//				synchronized public void run() {
//					netti = ((QueryNet)(clientQ.doQuery(new QueryNet()))).net;
//					this.notifyAll();
//				};
//			};
//			
////			netti = ((QueryNet)(clientQ.doQuery(new QueryNet()))).net;
//			new Thread(null,loader, "netLoader", 25*1024*1024).start();
//			try {
//				synchronized (loader) {
//					loader.wait();	
//				}
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}

	public SortedSet<Integer> getIdSet() {
		QueryIdSet qid = (QueryIdSet) clientQ.doQuery(new QueryIdSet());
		return qid.idSet;
	}

	public Person getPerson(int id) {
		QueryPerson qid = (QueryPerson) clientQ.doQuery(new QueryPerson(id));
		return qid.person;
	}

}

