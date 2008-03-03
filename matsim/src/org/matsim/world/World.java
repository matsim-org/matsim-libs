/* *********************************************************************** *
 * project: org.matsim.*
 * World.java
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

package org.matsim.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.events.Events;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.plans.Plans;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.algorithms.WorldAlgorithm;


public class World {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String name = null;

	private final TreeMap<IdI, Layer> layers = new TreeMap<IdI, Layer>();
	private final TreeMap<String, MappingRule> rules = new TreeMap<String, MappingRule>();

	private Layer top_layer = null;
	private Layer bottom_layer = null;

	private final ArrayList<WorldAlgorithm> algorithms = new ArrayList<WorldAlgorithm>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public World() {
	}

	//////////////////////////////////////////////////////////////////////
	// complete
	//////////////////////////////////////////////////////////////////////

	private final void completeZoneLayers() {
		// get zone layers
		ArrayList<ZoneLayer> zlayers = new ArrayList<ZoneLayer>();
		Iterator<IdI> lid_it = this.layers.keySet().iterator();
		while (lid_it.hasNext()) {
			IdI lid = lid_it.next();
			if (lid != NetworkLayer.LAYER_TYPE && lid != Facilities.LAYER_TYPE) {
				zlayers.add((ZoneLayer)this.layers.get(lid));
			}
		}

		if (zlayers.size() > 0) {
			if (zlayers.size() != (this.rules.size()+1)) { Gbl.errorMsg("This should never happen!"); }
			
			// find the top layer
			this.top_layer = null;
			for (int i=0; i<zlayers.size(); i++) {
				Layer l = zlayers.get(i);
				if (zlayers.get(i).getUpRule() == null) { this.top_layer = l; }
			}
			if (this.top_layer == null) { Gbl.errorMsg("Something is completely wrong!"); }
			
			// find the bottom layer
			int step_cnt = 1;
			this.bottom_layer = top_layer;
			while (this.bottom_layer.getDownRule() != null) {
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
				step_cnt++;
			}
			if (step_cnt != zlayers.size()) { Gbl.errorMsg("Something is completely wrong!"); }

		}
		else {
			if (!this.rules.isEmpty()) { Gbl.errorMsg("This should never happen!"); }
			this.top_layer = null;
			this.bottom_layer = null;
		}
	}
	
	public final void complete() {
		// first, remove rules and mappings containing network and/or facility layers
		Layer f_layer = this.layers.get(Facilities.LAYER_TYPE);
		Layer n_layer = this.layers.get(NetworkLayer.LAYER_TYPE);
		Iterator<Layer> l_it = this.layers.values().iterator();
		while (l_it.hasNext()) {
			Layer l = l_it.next();
			if (f_layer != null) {
				this.removeMappingRule(f_layer.getType(),l.getType());
				this.removeMappingRule(l.getType(),f_layer.getType());
			}
			if (n_layer != null) {
				this.removeMappingRule(n_layer.getType(),l.getType());
				this.removeMappingRule(l.getType(),n_layer.getType());
			}
		}
		
		// second, complete the zone layers
		this.completeZoneLayers();
		
		// third, create rules and mappings for the facility layer
		if (f_layer != null) {
			if (this.bottom_layer == null) {
				// no zone layers exist
				this.top_layer = this.bottom_layer = f_layer;
			}
			else {
				// TODO [balmermi] actually we could defined a specific mapping rule,
				// but then we need to set the mappings also.
				// So, i'm too lazy and define the mapping "m-m".
				this.createMappingRule(f_layer.getType().toString() + "[m]-[m]" + bottom_layer.getType().toString());
				// same as this.bottom_layer = f_layer, but with error checks
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
			}
		}
		
		// forth, create rules and mappings for the net layer
		if (n_layer != null) {
			if (this.bottom_layer == null) {
				// no zone nor facility layer exist
				this.top_layer = this.bottom_layer = n_layer;
			}
			else if (this.bottom_layer.getType() == Facilities.LAYER_TYPE) {
				// TODO [balmermi] a facility belongs to exactly one link
				// and a link contains zero, one or may facilities. The actual
				// mapping needs to be done later (as a MATSim-FUSION module)
				this.createMappingRule(n_layer.getType().toString() + "[1]-[*]" + bottom_layer.getType().toString());
				// same as this.bottom_layer = n_layer, but with error checks
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
			}
			else {
				// TODO [balmermi] actually we could defined a specific mapping rule,
				// but then we need to set the mappings also.
				// So, i'm too lazy and define the mapping "m-m".
				// TODO [balmermi] actually it does not make that sense
				// to connect the network with a zone layer anymore, since the
				// facility layer shoulkd be in between. Well... we will change
				// the whole strucutre anyway...
				this.createMappingRule(n_layer.getType().toString() + "[m]-[m]" + bottom_layer.getType().toString());
				// same as this.bottom_layer = n_layer, but with error checks
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	private final boolean removeMappingRule(final IdI l1_id, final IdI l2_id) {
		if ((this.layers.get(l1_id) == null) || (this.layers.get(l2_id) == null)) { return false; }
		Layer down_layer = null;
		Layer up_layer = null;
		if (this.getMappingRule(l1_id,l2_id) != null) {
			down_layer = this.layers.get(l1_id);
			up_layer = this.layers.get(l2_id);
		}
		else if (this.getMappingRule(l2_id,l1_id) != null) {
			up_layer = this.layers.get(l1_id);
			down_layer = this.layers.get(l2_id);
		}
		else {
			return true;
		}
		down_layer.removeUpRule();
		up_layer.removeDownRule();
		if (this.rules.remove(down_layer.getType().toString() + up_layer.getType().toString()) == null) {
			Gbl.errorMsg("This should never happen!");
		}
		return true;
	}
	
	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Layer createLayer(final IdI type, final String name) {
		if (this.layers.containsKey(type)) { Gbl.errorMsg("Layer type=" + type + " already exixts."); }
		if (type.equals(Facilities.LAYER_TYPE)) { return this.createFacilityLayer(); }
		if (type.equals(NetworkLayer.LAYER_TYPE)) { return this.createNetworkLayer(); }
		return this.createZoneLayer(type,name);
	}

	public final Layer createLayer(final String type, final String name) {
		return this.createLayer(new Id(type),name);
	}
	
	public final MappingRule createMappingRule(final String mapping_rule) {
		MappingRule m = new MappingRule(mapping_rule,this.layers);
		this.rules.put(m.getDownLayer().getType().toString() + m.getUpLayer().getType().toString(), m);
		return m;
	}

	private final ZoneLayer createZoneLayer(final IdI type,final String name) {
		ZoneLayer l = new ZoneLayer(type,name);
		this.layers.put(l.getType(),l);
		return l;
	}
	
	private final Facilities createFacilityLayer() {
		Facilities f = new Facilities();
		this.setFacilityLayer(f);
		return f;
	}
	
	private final NetworkLayer createNetworkLayer() {
		NetworkLayer n = NetworkLayerBuilder.newNetworkLayer();
		setNetworkLayer(n);
		return n;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setFacilityLayer(final Facilities facilitylayer) {
		if (facilitylayer == null) { Gbl.errorMsg("facilitylayer=null not allowed!"); }
		else {
			this.layers.put(Facilities.LAYER_TYPE,facilitylayer);
			this.complete();
		}
	}

	public void setNetworkLayer(final NetworkLayer network) {
		if (network == null) { Gbl.errorMsg("network=null not allowed!"); }
		else {
			this.layers.put(NetworkLayer.LAYER_TYPE,network);
			this.complete();
		}
	}
	
	protected final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final void addAlgorithm(final WorldAlgorithm algo) {
		this.algorithms.add(algo);
	}

	public final void addMapping(final Zone zone1, final Zone zone2) {
		if (this.getMappingRule(zone1.getLayer(),zone2.getLayer()) != null) {
			// zone1 = down_zone; zone2 = up_zone
			zone1.addUpMapping(zone2);
			zone2.addDownMapping(zone1);
		} else if (this.getMappingRule(zone2.getLayer(),zone1.getLayer()) != null) {
			// zone1 = up_zone; zone2 = down_zone
			zone1.addDownMapping(zone2);
			zone2.addUpMapping(zone1);
		} else {
			Gbl.warningMsg(this.getClass(), "addMapping(...)", Gbl.getWorld().toString() + "[zone1=" + zone1 + ",zone2=" + zone2 + " mapping rule not found]");
		}
	}

	protected final void addMapping(final MappingRule mapping_rule, final String down_zone_id, final String up_zone_id) {
		Zone down_zone = (Zone)mapping_rule.getDownLayer().getLocation(down_zone_id);
		Zone up_zone   = (Zone)mapping_rule.getUpLayer().getLocation(up_zone_id);
		if (down_zone == null) {
			Gbl.errorMsg(Gbl.getWorld().toString() + "[mapping_rule=" + mapping_rule + ",down_zone_id=" + down_zone_id + " down_zone does not exist]");
		}
		if (up_zone == null) {
			Gbl.errorMsg(Gbl.getWorld().toString() + "[mapping_rule=" + mapping_rule + ",up_zone_id=" + up_zone_id + " down_zone does not exist]");
		}
		down_zone.addUpMapping(up_zone);
		up_zone.addDownMapping(down_zone);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() {
		for (int i = 0; i < this.algorithms.size(); i++) {
			WorldAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getName() {
		return this.name;
	}

	public final Layer getLayer(final IdI layer_type) {
		return this.layers.get(layer_type);
	}
	
	public final Layer getLayer(final String layer_type) {
		return this.layers.get(new Id(layer_type));
	}

	public final TreeMap<IdI,Layer> getLayers() {
		return this.layers;
	}

	public final TreeMap<String,MappingRule> getRules() {
		return this.rules;
	}
	
	protected final MappingRule getMappingRule(final IdI down_id, final IdI up_id) {
		return this.rules.get(down_id.toString() + up_id.toString());
	}
	
	protected final MappingRule getMappingRule(final Layer down_layer, final Layer up_layer) {
		return this.getMappingRule(down_layer.getType(),up_layer.getType());
	}

	public final Layer getBottomLayer() {
		if ((this.bottom_layer == null) && !this.layers.isEmpty()) {
			Gbl.errorMsg("bottom_layer = null while world contains layers!");
		}
		return this.bottom_layer;
	}

	public final Layer getTopLayer() {
		if ((this.top_layer == null) && !this.layers.isEmpty()) {
			Gbl.errorMsg("top_layer = null while world contains layers!");
		}
		return this.top_layer;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_layers=" + this.layers.size() + "]" +
				"[nof_rules=" + this.rules.size() + "]" +
				"[top_layer=" + this.top_layer + "]" +
				"[bottom_layer=" + this.bottom_layer + "]" +
				"[nof_algorithms=" + this.algorithms.size() + "]";
	}

	//////////////////////////////////////////////////////////////////////
	// new methods and variables for globally handling pop/event/sim
	//////////////////////////////////////////////////////////////////////

	private Plans population;
	private Events events;

	public void setPopulation(final Plans population) {
		this.population = population;
	}
	public Plans getPopulation() {
		return this.population;
	}

	public void setEvents(final Events events) {
		this.events = events;
	}
	public Events getEvents() {
		return this.events;
	}

}
