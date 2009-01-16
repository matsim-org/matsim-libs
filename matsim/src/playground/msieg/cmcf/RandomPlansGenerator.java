/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlanGenerator.java
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

package playground.msieg.cmcf;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkReader;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.PopulationWriterHandlerImplV4;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.geometry.Coord;
import org.xml.sax.SAXException;

/**
 * This class is designed to create random plan files for
 * a given network.
 * 
 * @author msieg
 *
 */
public class RandomPlansGenerator {

	protected NetworkLayer network;
	
	private Random rand;
	
	private String networkFile = "examples/siouxfalls/network.xml";
	
	public RandomPlansGenerator(){
		this.setSeed(this.hashCode());
		try {
			this.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setSeed(long seed){
		if(rand == null)
			rand = new Random(seed);
		else
			rand.setSeed(seed);
	}

	public void read() throws IOException {
		this.network = new NetworkLayer();
		MatsimNetworkReader netReader = new MatsimNetworkReader( this.network );
		try {
			netReader.parse(this.networkFile);
			this.network.connect();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			throw e;
		}
	}
	
	public Population createPlans(int numberOfAgents, int startTime, int endTime){
		Population pop = new Population(Population.NO_STREAMING);
		
		for(int i=1; i <= numberOfAgents; i++){
			Person person = ((Person) new PersonImpl(new IdImpl(i)));
			Plan plan = new Plan(person);
			person.addPlan(plan);
			
			//create random home and work link
			Link lHome = this.getRandomLink(), lWork = lHome;
			while(lHome == lWork)
				lWork = this.getRandomLink();
			Coord homeCoord = lHome.getCenter(), workCoord = lWork.getCenter();
			
			//create home act
			Act a = plan.createAct("h", homeCoord);
			a.setLink(lHome);
			a.setEndTime(endTime);
			
			//create leg to work
			Leg leg = plan.createLeg(Mode.car);
			CarRoute route = new NodeCarRoute(lHome, lWork);
			//route.setNodes(...)
			leg.setRoute(route);
			
			//create work act
			a = plan.createAct("w", workCoord);
			a.setLink(lWork);
			a.setDuration(3 * 3600);
		
			//finally add person to population instance
			pop.addPerson(person);
		}
		
		return pop;
	}
	
	private Link getRandomLink(){
		int i = this.rand.nextInt(this.network.getLinks().size());
		Link randomLink = null;
		for (Iterator<Link> iterator = this.network.getLinks().values().iterator(); i >= 0; i--) {
			randomLink = (Link) iterator.next();
		}
		return randomLink;
	}
	
	
	public void writePlans(Population plans){
		PopulationWriter pwriter = new PopulationWriter(plans);
		pwriter.setWriterHandler(new PopulationWriterHandlerImplV4());
		pwriter.write();
	}
	
	public void writePlans(Population plans, String file){
		PopulationWriter pwriter = new PopulationWriter(plans, file, "v4", 1.0);
		pwriter.setWriterHandler(new PopulationWriterHandlerImplV4());
		pwriter.write();
	}
	
	public static void main(String[] args) {
		RandomPlansGenerator rpg = new RandomPlansGenerator();
		Population pop = rpg.createPlans(3, 0, 1);
		System.out.println(pop);
		rpg.writePlans(pop, "poptest.txt");
	}
}

