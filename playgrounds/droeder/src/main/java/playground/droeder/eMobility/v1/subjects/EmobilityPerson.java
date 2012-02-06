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
package playground.droeder.eMobility.v1.subjects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

import playground.droeder.DaFileReader;

/**
 * @author droeder
 *
 */
public class EmobilityPerson {
	private static final Logger log = Logger.getLogger(EmobilityPerson.class);
	
	private Id id;
	private Coord start;
	private EmobVehicle veh;
	private List<Appointment> appointments;
	private Iterator<Appointment> iterator;
	private Appointment currentAppointment =null;
	private Network net;
	private Coord firstPark;
	
	public EmobilityPerson(Id id, Coord start, Node vehStart, Double vehCharge, Network net){
		this.id = id;
		this.start = start;
		this.firstPark = vehStart.getCoord();
		this.veh = new EmobVehicle(id, vehCharge, vehStart.getCoord());
		this.appointments = new ArrayList<Appointment>();
		this.net = net;
		this.iterator = this.appointments.iterator();
	}
	
	public Id getId(){
		return this.id;
	}
	
	public Appointment getCurrentAppointment(){
		if(this.currentAppointment.finished()){
			if(this.iterator.hasNext()){
				this.currentAppointment = iterator.next(); 
			}else{
				log.error("should not happen...");
			}
		}
		return this.currentAppointment;
	}

	public boolean readDataFromFile(String planFile){
		Set<String[]> lines = DaFileReader.readFileContent(planFile, "\t", true);
		
		Appointment app;
		Coord start, end = null;
		boolean first = true;
		for(String[] s: lines){
			if(end == null){
				start = this.start;
			}else{
				start = end;
			}
			end = net.getNodes().get(new IdImpl(s[0])).getCoord();
			String beginn = s[1].substring(4);
			String finish = s[2].substring(4);
			app = new Appointment(start, end, Time.parseTime(beginn), Time.parseTime(finish));
			if(first){
				app.setParking1(this.firstPark);
				first = false;
			}
			app.setDisCharging(new IdImpl(s[4]));
			app.setCharging(new IdImpl(s[6]));
			//TODO use other parameters
			this.appointments.add(app);
		}
		this.iterator = this.appointments.iterator();
		if(this.iterator.hasNext()){
			this.currentAppointment = this.iterator.next();
			return true;
		}else{
			//there is no plan
			return false;
		}
	}
	
	public EmobVehicle getVehicle(){
		return this.veh;
	}
	
	public Person createMatsimPerson(PopulationFactory fac){
		Plan plan = fac.createPlan();
		Person p = fac.createPerson(this.id);
		
		Appointment previousAppointment = null;
		for(Appointment app : this.appointments){
			for(PlanElement e : app.createMatsimPlanelements(fac, previousAppointment)){
				if(e instanceof Leg){
					plan.addLeg((Leg) e);
				}else if(e instanceof Activity){
					plan.addActivity((Activity) e);
				}
			}
			previousAppointment = app;
		}
		p.addPlan(plan);
		return p;
	}
	
	public static void main(String[] args){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile("D:/VSP/svn/shared/iv_counts/network-base_ext.xml.gz");
		
		
		EmobilityPerson agent = new EmobilityPerson(new IdImpl("emob1"), new CoordImpl(4588309,5820079), sc.getNetwork().getNodes().get(new IdImpl("26736131")), 26.0, sc.getNetwork());
		agent.readDataFromFile("C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/testplan.txt");
		sc.getPopulation().addPerson(agent.createMatsimPerson(sc.getPopulation().getFactory()));
		
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write("C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/testplan.xml");
	}
}
