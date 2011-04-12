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
package playground.droeder.bvg09.analysis.preProcess;

import java.util.ListIterator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.filters.AbstractPersonFilter;

import playground.droeder.DaPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author droeder
 *
 */
public class PersonLocationFilter extends AbstractPersonFilter{
	private Population pop;
	private int b;
	private int b2brb;
	private int brb;
	private Geometry g;
	private GeometryFactory fac;
	private int neededSize;
	private int nextMsg = 1;
	private int i = 1;
	private Network net;

	public PersonLocationFilter(Geometry g, int size, Network net){
		super();
		this.reset();
		this.g = g;
		this.fac = new GeometryFactory();
		this.neededSize = size;
		this.net = net;
	}
	public void reset(){
		this.pop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		this.b = 0;
		this.brb = 0;
		this.b2brb = 0;
	}
	
	public void run(Person person){
		if(judge(person)){
			this.count();
		}
	}
	
	@Override
	public boolean judge(Person p) {
		boolean b = false;
		boolean brb = false;
		boolean b2brb = false;
		
		if(p.getSelectedPlan().getPlanElements().size() < 3) return false;
		if(i == 10) System.exit(0);
		
		ListIterator<PlanElement> it = p.getSelectedPlan().getPlanElements().listIterator();
		PlanElement pe;
		PlanElement pe2 = null;
		
		
		while(it.hasNext()){
			pe = it.next();
			
			if(pe instanceof Activity ){
				pe2 = null;
				while(it.hasNext()){
					it.next();
					pe2 = it.next();
					if(!((Activity) pe2).getType().equals("pt interaction")) break;
				}
				if(pe2 == null){
					break;
				}else if(g.contains(this.fac.createPoint(new Coordinate(((Activity) pe).getCoord().getX(), ((Activity) pe).getCoord().getY()))) &&
						g.contains(this.fac.createPoint(new Coordinate(((Activity) pe2).getCoord().getX(), ((Activity) pe).getCoord().getY())))){
					b = true;
				}else if(g.contains(this.fac.createPoint(new Coordinate(((Activity) pe).getCoord().getX(), ((Activity) pe).getCoord().getY()))) ||
						g.contains(this.fac.createPoint(new Coordinate(((Activity) pe2).getCoord().getX(), ((Activity) pe).getCoord().getY())))){
					b2brb = true;
				}else if(!g.contains(this.fac.createPoint(new Coordinate(((Activity) pe).getCoord().getX(), ((Activity) pe).getCoord().getY()))) &&
						!g.contains(this.fac.createPoint(new Coordinate(((Activity) pe2).getCoord().getX(), ((Activity) pe).getCoord().getY())))){
					brb = true;
				}
				it.previous();
			}
		}
		
		if(brb = true && this.brb < (this.neededSize/3 + 1)){
			this.pop.addPerson(p);
		}else if(b2brb = true && this.b2brb <(this.neededSize/3 + 1)){
			this.pop.addPerson(p);
		}else if(b = true && this.b < (this.neededSize/3 + 1)){
			this.pop.addPerson(p);
		}else{
			return false;
		}
		
		
		if(b == true && brb == true && b2brb == true){
			this.b++;
			this.brb++;
			this.b2brb++;
		}else if(b == false && brb == true && b2brb == true){
			this.brb++;
			this.b2brb++;
		}else if(b == true && brb == false && b2brb == true){
			this.b++;
			this.b2brb++;
		}else if(b == true && brb == true && b2brb == false){
			this.b++;
			this.brb++;
		}else if(b == false && brb == false && b2brb == true){
			this.b2brb++;
		}else if(b == true && brb == false && b2brb == false){
			this.b++;
		}else if(b == false && brb == true && b2brb == false){
			this.brb++;
		}
		
		if(this.pop.getPersons().size() % this.nextMsg == 0){
		System.out.println("population size: " + this.pop.getPersons().size()+ ", b: " + this.b + ", brb: " + this.brb + ", b2brb: " + this.b2brb);
		this.nextMsg += 50;
	}
		
		if((this.b + this.brb + this.b2brb) >= this.neededSize){
			new PopulationWriter(this.pop, this.net).writeV4(DaPaths.VSP + "BVG09_Auswertung/testPopulation" + this.i + ".xml");
			this.reset();
			this.i++;
		}
		
		if(b == true || brb == true || b2brb == true){
			return true;
		}else{
			return false;
		}
//		for(PlanElement pe: p.getSelectedPlan().getPlanElements()){
//			if (pe instanceof Activity){
//				if(g.contains(this.fac.createPoint(new Coordinate(((Activity) pe).getCoord().getX(), ((Activity) pe).getCoord().getY())))){
//					b = true;
//				}else if(!g.contains(this.fac.createPoint(new Coordinate(((Activity) pe).getCoord().getX(), ((Activity) pe).getCoord().getY())))){
//					brb = true;
//				}
//			}
//		}
//		if(this.pop.getPersons().size() % this.nextMsg == 0){
//			System.out.println("population size: " + this.pop.getPersons().size()+ ", b: " + this.b + ", brb: " + this.brb + ", b2brb: " + this.b2brb);
//			this.nextMsg += 50;
////			System.gc();
//		}
//		if((b == false) && (brb == false)){
//			return false;
//		}else if ((this.b + this.brb + this.b2brb) < this.neededSize){
//			
//			if((b == true) && (brb == false) && (this.b < (this.neededSize/3))){
//				this.b++;
//				this.pop.addPerson(p);
//				return true;
//			} else if((b == true) && (brb == true) && (this.b2brb <(this.neededSize/3))){
//				this.b2brb++;
//				this.pop.addPerson(p);
//				return true;
//			}else if((b == false) && (brb == true) && this.brb <(this.neededSize/3)){
//				this.brb++;
//				this.pop.addPerson(p);
//				return true;
//			}else{
//				return false;
//			}
//		}else if((this.b + this.brb + this.b2brb) == (this.neededSize - 2)){
//			new PopulationWriter(this.pop, this.net).writeV4(DaPaths.VSP + "BVG09_Auswertung/testPopulation.xml");
//			System.exit(0);
//			return false;
//		}else{
//			return false;
//		}
	}
}
