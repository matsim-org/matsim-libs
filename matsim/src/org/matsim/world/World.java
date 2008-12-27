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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;
import org.matsim.world.algorithms.WorldConnectLocations;

public class World {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String name = null;

	private final TreeMap<Id, Layer> layers = new TreeMap<Id, Layer>();
	private final TreeMap<String, MappingRule> rules = new TreeMap<String, MappingRule>();

	private Layer top_layer = null;
	private Layer bottom_layer = null;

	private final static Logger log = Logger.getLogger(World.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// complete
	//////////////////////////////////////////////////////////////////////

	public final void complete() {
		complete(new HashSet<String>());
	}
	
	public final void complete(Set<String> excludingLinkTypes) {
		// 1. remove rules and mappings containing network and/or facility layers
		Layer f_layer = this.layers.get(Facilities.LAYER_TYPE);
		Layer n_layer = this.layers.get(NetworkLayer.LAYER_TYPE);
		for (Layer l : layers.values()) {
			if (f_layer != null) {
				this.removeMappingRule(f_layer.getType(),l.getType());
				this.removeMappingRule(l.getType(),f_layer.getType());
			}
			if (n_layer != null) {
				this.removeMappingRule(n_layer.getType(),l.getType());
				this.removeMappingRule(l.getType(),n_layer.getType());
			}
		}
		// 2. complete the zone layers
		// 2.1. get the zone layers
		ArrayList<ZoneLayer> zlayers = new ArrayList<ZoneLayer>();
		Iterator<Id> lid_it = this.layers.keySet().iterator();
		while (lid_it.hasNext()) {
			Id lid = lid_it.next();
			if ((lid != NetworkLayer.LAYER_TYPE) && (lid != Facilities.LAYER_TYPE)) {
				zlayers.add((ZoneLayer)this.layers.get(lid));
			}
		}
		// 2.2 set the top and bottom layer reference
		if (zlayers.size() == 0) {
			if (!this.rules.isEmpty()) { throw new RuntimeException("data inconsistency"); }
			this.top_layer = null;
			this.bottom_layer = null;
		}
		else { // zlayers.size() > 0
			if (zlayers.size() != (this.rules.size()+1)) { throw new RuntimeException("data inconsistency"); }
			// 2.2.1. find the top layer
			this.top_layer = null;
			for (int i=0; i<zlayers.size(); i++) {
				Layer l = zlayers.get(i);
				if (zlayers.get(i).getUpRule() == null) { this.top_layer = l; }
			}
			if (this.top_layer == null) { throw new RuntimeException("data inconsistency"); }
			// 2.2.2. find the bottom layer
			int step_cnt = 1;
			this.bottom_layer = this.top_layer;
			while (this.bottom_layer.getDownRule() != null) {
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
				step_cnt++;
			}
			if (step_cnt != zlayers.size()) { throw new RuntimeException("data inconsistency"); }
		}
		// 3. create mapping rule (but no zone<->facility mappings) for the facility layer
		if (f_layer != null) {
			if (this.bottom_layer == null) { // no zone layers exist
				this.top_layer = this.bottom_layer = f_layer;
			}
			else {
				this.createMappingRule(f_layer.getType().toString() + "[m]-[m]" + this.bottom_layer.getType().toString());
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
			}
		}
		// 4. create mapping rule (but no facility<->link mappings) for the net layer
		if (n_layer != null) {
			if (this.bottom_layer == null) { // no zone nor facility layer exist
				this.top_layer = this.bottom_layer = n_layer;
			}
//			else if (this.bottom_layer.getType() == Facilities.LAYER_TYPE) {
//				this.createMappingRule(n_layer.getType().toString() + "[m]-[m]" + this.bottom_layer.getType().toString());
//				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
//			}  // it's the same as the "else" below
			else {
				this.createMappingRule(n_layer.getType().toString() + "[m]-[m]" + this.bottom_layer.getType().toString());
				this.bottom_layer = this.bottom_layer.getDownRule().getDownLayer();
			}
		}
		// 5. connect the locations from neighbor layers
		new WorldConnectLocations(excludingLinkTypes).run(this);
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	private final boolean removeMappingRule(final Id l1_id, final Id l2_id) {
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
			throw new RuntimeException("This should never happen!");
		}
		return true;
	}

	public final boolean removeMapping(Location loc1, Location loc2) {
		if (this.getMappingRule(loc1.getLayer(),loc2.getLayer()) != null) {
			// loc1 = down_loc; loc2 = up_loc
			if (loc1.getUpMapping().containsKey(loc2.getId()) && loc2.getDownMapping().containsKey(loc1.getId())) {
				loc1.getUpMapping().remove(loc2.getId());
				loc2.getDownMapping().remove(loc1.getId());
				return true;
			}
			else {
				log.warn("loc1="+loc1+",loc2="+loc2+": mapping not found.");
				return false;
			}
		} else if (this.getMappingRule(loc2.getLayer(),loc1.getLayer()) != null) {
			// loc1 = up_loc; loc2 = down_loc
			if (loc1.getDownMapping().containsKey(loc2.getId()) && loc2.getUpMapping().containsKey(loc1.getId())) {
				loc1.getDownMapping().remove(loc2.getId());
				loc2.getUpMapping().remove(loc1.getId());
				return true;
			}
			else {
				log.warn("loc1="+loc1+",loc2="+loc2+": mapping not found.");
				return false;
			}
		} else {
			log.warn("loc1="+loc1+",loc2="+loc2+": mapping rule not found.");
			return false;
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Layer createLayer(final Id type, final String name) {
		if (this.layers.containsKey(type)) {
			throw new IllegalArgumentException("Layer type=" + type + " already exixts.");
		}
		if (type.equals(Facilities.LAYER_TYPE)) { return this.createFacilityLayer(); }
		if (type.equals(NetworkLayer.LAYER_TYPE)) { return this.createNetworkLayer(); }
		return this.createZoneLayer(type,name);
	}

	public final MappingRule createMappingRule(final String mapping_rule) {
		MappingRule m = new MappingRule(mapping_rule,this.layers);
		this.rules.put(m.getDownLayer().getType().toString() + m.getUpLayer().getType().toString(), m);
		return m;
	}

	private final ZoneLayer createZoneLayer(final Id type,final String name) {
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
		NetworkLayer n = new NetworkLayer();
		setNetworkLayer(n);
		return n;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setFacilityLayer(final Facilities facilityLayer) {
		if (facilityLayer == null) { 
			throw new IllegalArgumentException("facilityLayer=null not allowed!");
		}
		this.layers.put(Facilities.LAYER_TYPE, facilityLayer);
	}

	public void setNetworkLayer(final NetworkLayer network) {
		if (network == null) {
			throw new IllegalArgumentException("network=null not allowed!");
		}
		this.layers.put(NetworkLayer.LAYER_TYPE, network);
	}

	protected final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////
	
	public final boolean addMapping(Location loc1, Location loc2) {
		if (this.getMappingRule(loc1.getLayer(),loc2.getLayer()) != null) {
			// loc1 = down_loc; loc2 = up_loc
			loc1.addUpMapping(loc2);
			loc2.addDownMapping(loc1);
		} else if (this.getMappingRule(loc2.getLayer(),loc1.getLayer()) != null) {
			// loc1 = up_loc; loc2 = down_loc
			loc1.addDownMapping(loc2);
			loc2.addUpMapping(loc1);
		} else {
			log.warn(this.toString() + "[loc1=" + loc1 + ",loc2=" + loc2 + " mapping rule not found]");
			return false;
		}
		return true;
	}

	protected final void addMapping(final MappingRule mapping_rule, final String down_zone_id, final String up_zone_id) {
		Zone down_zone = (Zone)mapping_rule.getDownLayer().getLocation(down_zone_id);
		Zone up_zone   = (Zone)mapping_rule.getUpLayer().getLocation(up_zone_id);
		if (down_zone == null) {
			throw new RuntimeException(this.toString() + "[mapping_rule=" + mapping_rule + ",down_zone_id=" + down_zone_id + " down_zone does not exist]");
		}
		if (up_zone == null) {
			throw new RuntimeException(this.toString() + "[mapping_rule=" + mapping_rule + ",up_zone_id=" + up_zone_id + " down_zone does not exist]");
		}
		down_zone.addUpMapping(up_zone);
		up_zone.addDownMapping(down_zone);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getName() {
		return this.name;
	}

	public final Layer getLayer(final Id layer_type) {
		return this.layers.get(layer_type);
	}

	public final Layer getLayer(final String layer_type) {
		return this.layers.get(new IdImpl(layer_type));
	}

	public final TreeMap<Id,Layer> getLayers() {
		return this.layers;
	}

	public final TreeMap<String,MappingRule> getRules() {
		return this.rules;
	}

	protected final MappingRule getMappingRule(final Id down_id, final Id up_id) {
		return this.rules.get(down_id.toString() + up_id.toString());
	}

	protected final MappingRule getMappingRule(final Layer down_layer, final Layer up_layer) {
		return this.getMappingRule(down_layer.getType(),up_layer.getType());
	}

	public final Layer getBottomLayer() {
		if ((this.bottom_layer == null) && !this.layers.isEmpty()) {
			throw new RuntimeException("bottom_layer = null while world contains layers!");
		}
		return this.bottom_layer;
	}

	public final Layer getTopLayer() {
		if ((this.top_layer == null) && !this.layers.isEmpty()) {
			throw new RuntimeException("top_layer = null while world contains layers!");
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
				"[bottom_layer=" + this.bottom_layer + "]";
	}

}
