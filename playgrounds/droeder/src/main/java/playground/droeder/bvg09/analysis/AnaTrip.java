/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.bvg09.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class AnaTrip {
	private String type;
	private Coordinate start;
	private Coordinate end;
	private ArrayList<PersonEvent> events;
	private ArrayList<PlanElement> elements;
	
	public AnaTrip(ArrayList<PersonEvent> events, ArrayList<PlanElement> elements){
		this.events = events;
		this.elements = elements;
		this.start = new Coordinate(((Activity) elements.get(0)).getCoord().getX(), 
				((Activity) elements.get(0)).getCoord().getY());
		this.end = new Coordinate(((Activity) elements.get(elements.size() - 1)).getCoord().getX(), 
				((Activity) elements.get(elements.size() - 1)).getCoord().getY());
	}
	
	public void analyze(){
		
	}
	

	/**
	 * @return
	 */
	public Geometry getStart() {
		return new GeometryFactory().createPoint(this.start);
	}

	/**
	 * @return
	 */
	public Geometry getEnd() {
		return new GeometryFactory().createPoint(this.end);
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		for(PlanElement pe: this.elements){
			if(pe instanceof Leg){
				buffer.append(((Leg) pe).getMode() + "\t");
			}else if (pe instanceof Activity){
				buffer.append(((Activity) pe).getType() + "\t");
			}
		}
		buffer.append("\n");
		for(PersonEvent pe : this.events){
			if(pe instanceof ActivityEvent){
				buffer.append(((ActivityEvent) pe).getActType() + "\t");
			}else if(pe instanceof AgentEvent){
				buffer.append(((AgentEvent) pe).getLegMode()+ "\t");
			}
		}
		
		return buffer.toString();
	}
}
