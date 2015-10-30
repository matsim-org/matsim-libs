/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.visum;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import playground.johannes.gsv.visum.NetFileReader.TableHandler;

import java.util.*;

/**
 * @author johannes
 *
 */
public class LinkTableHandler extends TableHandler {

	public static final String FROM_KEY = "VONKNOTNR";
	
	public static final String TO_KEY = "NACHKNOTNR";
	
	public static final String LENGTH_KEY = "LAENGE";
	
	public static final String CAPACITY_KEY = "KAPIV";
	
	public static final String FREESPEED_KEY = "V0IV";
	
	public static final String LANES_KEY = "ANZFAHRSTREIFEN";
	
	public static final String MODES_KEY = "VSYSSET";
	
	private final Network network;
	
	private List<UnitTransformation> unitTransformations;
	
	private final Map<String, String> modeMappings;
	
	private IdGenerator idGenerator = new IdentityIdGenerator();
	
	public LinkTableHandler(Network network, Map<String, String> modeMappings) {
		this(network, modeMappings, null);
		unitTransformations = new ArrayList<UnitTransformation>(2);
		//FIXME the order is decisive, not an elegant solution
		unitTransformations.add(KmhUnitTransformation.getInstance());
		unitTransformations.add(KmUnitTransformation.getInstance());
	}
	
	public LinkTableHandler(Network network, Map<String, String> modeMappings, List<UnitTransformation> transforms) {
		this.network = network;
		this.modeMappings = modeMappings;
		this.unitTransformations = transforms;
	}
	
	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.visum.NetFileReader.TableHandler#handleRow(java.util.Map)
	 */
	@Override
	public void handleRow(Map<String, String> record) {
		Node from = network.getNodes().get(idGenerator.generateId(record.get(FROM_KEY), Node.class));
		Node to = network.getNodes().get(idGenerator.generateId(record.get(TO_KEY), Node.class));
		
		if(from != null && to != null) {
			Id<Link> id = idGenerator.generateId(record.get(NodeTableHandler.ID_KEY), Link.class);
			
			Link link = network.getLinks().get(id);
			if(link == null) {
				/*
				 * outward link
				 */
				link = network.getFactory().createLink(id, from, to);
			} else {
				/*
				 * link already exists, check if return link
				 */
				if(link.getFromNode().equals(to) && link.getToNode().equals(from)) {
					id = idGenerator.generateId(record.get(NodeTableHandler.ID_KEY) + "R", Link.class);
					link = network.getFactory().createLink(id, from, to);
				} else {
					throw new RuntimeException("Link with already existing id but not the return link.");
				}
			}
			
			network.addLink(link);
			/*
			 * length
			 */
			String value = record.get(LENGTH_KEY);
			for(UnitTransformation transform : unitTransformations) {
				if(value.contains(transform.getUnit())) {
					link.setLength(transform.transform(value));
					break;
				}
			}
			/*
			 * capacity
			 */
			link.setCapacity(Double.parseDouble(record.get(CAPACITY_KEY))); //TODO dunno what unit
			/*
			 * freespeed
			 */
			value = record.get(FREESPEED_KEY);
			for(UnitTransformation transform : unitTransformations) {
				if(value.contains(transform.getUnit())) {
					link.setFreespeed(transform.transform(value));
					break;
				}
			}
			/*
			 * lanes
			 */
			link.setNumberOfLanes(Double.parseDouble(record.get(LANES_KEY)));
			/*
			 * modes
			 */
			String tokens[] = record.get(MODES_KEY).split(",");
			Set<String> modes = new HashSet<String>(tokens.length);
			for(String token : tokens) {
				modes.add(modeMappings.get(token));
			}
			link.setAllowedModes(modes);
			
		} else {
			throw new RuntimeException("Either from or to node not found.");
		}
		
		
	}

}
