/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.acmarmol.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.HouseholdsImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleUtils;

import playground.acmarmol.utils.CoordConverter;

public class PopulationFromMZ {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////	
	private static final String HOME = "home";
	
	private static final String ALWAYS = "always";
	private static final String ARRANGEMENT = "by arrengement";
	private static final String NEVER = "never";
	private static final String NO_ANSWER = "no answer";
	private static final String UNSPECIFIED = "unspecified";
	private static final String NOT_KNOWN = "not known";
	
	private static final String YES = "yes";
	private static final String NO = "no";
	
	private final String populationInputfile;
	private final String wegeInputfile;
	private ObjectAttributes population_attributes;
	
	private Set<Id> coord_err_pids;
	private Set<Id> time_err_pids; 
	
//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////	
	
	public PopulationFromMZ(final String peopleInputfile, final String wegeInputfile) {
		super();
		this.populationInputfile = peopleInputfile;
		this.wegeInputfile = wegeInputfile;
		this.population_attributes  = new ObjectAttributes();
	}	
	
	
//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////	
	
	public void createPopulation(Population population) throws Exception{
		
		System.out.println("      parsing population from " + this.populationInputfile);	
		
		this.coord_err_pids = new HashSet<Id>();
		this.time_err_pids = new HashSet<Id>();
		
		FileReader fr = new FileReader(this.populationInputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
				
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		//household number
		String hhnr = entries[0].trim();
		
		//person number (zielpnr)
		String zielpnr = entries[1].trim();
		
		//person weight 
		String person_weight = entries[2];
		population_attributes.putAttribute(hhnr.concat(zielpnr), "person weight", person_weight);
		
		//person age 
		String age = entries[188];
		population_attributes.putAttribute(hhnr.concat(zielpnr), "age", age);
		
		//person gender
		String gender = entries[190];
		if(gender.equals("1")){gender = "male";}
		else if(gender.equals("2")){gender = "female";}
		else Gbl.errorMsg("This should never happen!  Gender: " + gender+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "gender", gender);
		
		//day of week
		String dow = entries[10];
		if(dow.equals("1")){dow = "monday";}
		else if(dow.equals("2")){dow = "tuesday";}else if(dow.equals("3")){dow = "wednesday";}
		else if(dow.equals("4")){dow = "thurdsday";}else if(dow.equals("5")){dow = "friday";}
		else if(dow.equals("6")){dow = "saturday";}else if(dow.equals("7")){dow = "sunday";}
		else Gbl.errorMsg("This should never happen!  Day of week: " + dow + " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "day of week", dow);

		
		//employment status
		boolean employed = true;		
		String employment_status = entries[177];
		
		if(!employment_status.equals(" ")){
		if(Integer.parseInt(employment_status)>4){employed = false;}
		}
				
		if(employment_status.equals("1")){employment_status = "independent";}
		else if(employment_status.equals("2")){employment_status = "Mitarbeitendes Familienmitglied";} 	else if(employment_status.equals("3")){employment_status = "employee";}
		else if(employment_status.equals("4")){employment_status = "apprentice-trainee"	;}				else if(employment_status.equals("5")){employment_status = "unemployed";}
		else if(employment_status.equals("6")){employment_status = "not in labor force";}				else if(employment_status.equals("7")){employment_status = "retired";}
		else if(employment_status.equals("8")){employment_status = "disabled";}							else if(employment_status.equals("9")){employment_status = "housewife/hosehusband";}
		else if(employment_status.equals("10")){employment_status = "other inactive";}					else if(employment_status.equals(" ")){employment_status = "unspecified";}
		else Gbl.errorMsg("This should never happen! Employment Status: " + employment_status + " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "work: employment status", employment_status);
		
		//level of employment
		String level_employment = entries[179];
		if(level_employment.equals("1")){level_employment = "90-100%";}
		else if(level_employment.equals("2")){level_employment = "70-89%";}			else if(level_employment.equals("3")){level_employment = "50-69%";}
		else if(level_employment.equals("4")){level_employment = "less than 50%";}	else if(level_employment.equals("99")){level_employment = "part-time unspecified";}
		else if(level_employment.equals("999")){level_employment = "unemployed";}	else if(level_employment.equals(" ")){level_employment = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen! Level of Employment: " + level_employment + " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "work: level of employment", level_employment);
		
		// work location coordinate (round to hectare) - WGS84 (124,125) & CH1903 (126,127)
		if(employed){
		Coord work_location = new CoordImpl(entries[126].trim(),entries[127].trim());
		work_location.setX(Math.round(work_location.getX()/100.0)*100);
		work_location.setY(Math.round(work_location.getY()/100.0)*100);
		population_attributes.putAttribute(hhnr.concat(zielpnr), "work: location coord", work_location);
		} //else?
		
		//car availability
		String car_av = entries[63];
		if(car_av.equals("1")){car_av = ALWAYS;}
		else if(car_av.equals("2")){car_av = ARRANGEMENT;}
		else if(car_av.equals("3")){car_av = NEVER;}
		else if(car_av.equals("-99")){car_av = "???";}// -review
		else if(car_av.equals("-98")){car_av = NO_ANSWER;}
		else if(car_av.equals("-97")){car_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Car availability: " + car_av+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "availability: car", car_av);
		
		//motorcycle availability
		String mcycle_av = entries[62];
		if(mcycle_av.equals("1")){mcycle_av = ALWAYS;}
		else if(mcycle_av.equals("2")){mcycle_av = ARRANGEMENT;}
		else if(mcycle_av.equals("3")){mcycle_av = NEVER;}
		else if(mcycle_av.equals("-99")){mcycle_av = "???";}// -review
		else if(mcycle_av.equals("-98")){mcycle_av = NO_ANSWER;}
		else if(mcycle_av.equals("-97")){mcycle_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Motorcycle availability: " + mcycle_av+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "availability: motorcycle", mcycle_av);
		
		//small motorcycle availability
		String smcycle_av = entries[61];
		if(smcycle_av.equals("1")){smcycle_av = ALWAYS;}
		else if(smcycle_av.equals("2")){smcycle_av = ARRANGEMENT;}
		else if(smcycle_av.equals("3")){smcycle_av = NEVER;}
		else if(smcycle_av.equals("-99")){smcycle_av = "age less than 16";}
		else if(smcycle_av.equals("-98")){smcycle_av = NO_ANSWER;}
		else if(smcycle_av.equals("-97")){smcycle_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Small motorcycle availability: " + smcycle_av+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "availability: small motorcycle ", smcycle_av);
		
		
		//Mofa availability
		String mofa_av = entries[60];
		if(mofa_av.equals("1")){mofa_av = ALWAYS;}
		else if(mofa_av.equals("2")){mofa_av = ARRANGEMENT;}
		else if(mofa_av.equals("3")){mofa_av = NEVER;}
		else if(mofa_av.equals("-99")){mofa_av = "age less than 14";}
		else if(mofa_av.equals("-98")){mofa_av = NO_ANSWER;}
		else if(mofa_av.equals("-97")){mofa_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Mofa availability: " + mofa_av+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "availability: mofa", mofa_av);
		
		//Bicycle availability
		String bike_av = entries[59];
		if(bike_av.equals("1")){bike_av = ALWAYS;}
		else if(bike_av.equals("2")){bike_av = ARRANGEMENT;}
		else if(bike_av.equals("3")){bike_av = NEVER;}
		else if(bike_av.equals("-99")){bike_av = UNSPECIFIED;}// -review
		else if(bike_av.equals("-98")){bike_av = NO_ANSWER;}
		else if(bike_av.equals("-97")){bike_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Bike availability: " + bike_av+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "availability: bicycle", bike_av);
		
		//car-sharing membership
		String sharing = entries[56];
		if(sharing .equals("1")){sharing  = YES;}
		else if(sharing.equals("2")){sharing  = NO;}
		else if(sharing.equals("-99")){sharing = "???";}// -review
		else if(sharing.equals("-98")){sharing = NO_ANSWER;}
		else if(sharing.equals("-97")){sharing = NOT_KNOWN;}	
		else Gbl.errorMsg("This should never happen!  Car sharing membership: " + sharing + " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "car sharing membership", sharing);
		
		//HalbTax
		String halbtax = entries[48];
		if(halbtax.equals("1")){halbtax = YES;}
		else if(halbtax.equals("2")){halbtax = NO;}
		else if(halbtax.equals("-98")){halbtax = NO_ANSWER;}
		else if(halbtax.equals("-97")){halbtax = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  Halbtax: " + halbtax+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Halbtax", halbtax);
		
		//GA first class
		String gaFirstClass = entries[49];
		if(gaFirstClass.equals("1")){gaFirstClass = YES;} 
		else if(gaFirstClass.equals("2")){gaFirstClass = NO;}
		else if(gaFirstClass.equals("-98")){gaFirstClass = NO_ANSWER;}
		else if(gaFirstClass.equals("-97")){gaFirstClass = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  GA First Class: " + gaFirstClass+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "abonnement: GA first class", gaFirstClass);
		
		//GA second class
		String gaSecondClass = entries[50];
		if(gaSecondClass.equals("1")){gaSecondClass = YES;}
		else if(gaSecondClass.equals("2")){gaSecondClass = NO;}
		else if(gaSecondClass.equals("-98")){gaSecondClass = NO_ANSWER;}
		else if(gaSecondClass.equals("-97")){gaSecondClass = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  GA Second Class: " + gaSecondClass+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "abonnement: GA second class", gaSecondClass);
		
		
		//verbund abonnement
		String verbund = entries[51];
		if(verbund.equals("1")){verbund = YES;}
		else if(verbund.equals("2")){verbund = NO;}
		else if(verbund.equals("-98")){verbund = NO_ANSWER;}
		else if(verbund.equals("-97")){verbund = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  Verbund abonnement: " + verbund+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Verbund", verbund);
		
		//strecken abonnement
		String strecken = entries[52];
		if(strecken.equals("1")){strecken = YES;}
		else if(strecken.equals("2")){strecken = NO;}
		else if(strecken.equals("-98")){strecken = NO_ANSWER;}
		else if(strecken.equals("-97")){strecken = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  GA Second Class: " + strecken+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Stecken", strecken);
		
		
		//Gleis 7
		String gleis7 = entries[53];
		if(gleis7.equals("1")){gleis7 = YES;}
		else if(gleis7.equals("2")){gleis7 = NO;}
		else if(gleis7.equals("-99")){gleis7 = "not in age";}
		else if(gleis7.equals("-98")){gleis7 = NO_ANSWER;}
		else if(gleis7.equals("-97")){gleis7 = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  Gleis 7: " + gleis7+ " doesn't exist");
		population_attributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Gleis 7", gleis7);
		
		
		//creating matsim person
		PersonImpl person = new PersonImpl(new IdImpl(hhnr.concat(zielpnr)));
		person.setAge(Integer.parseInt(age));
		person.setEmployed(employed);
		//person.setLicence(licence);
		person.setSex(gender);
		population.addPerson(person);
		}
	
	
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # persons parsed = "  + population.getPersons().size());
		System.out.println();
	
	
	}

//////////////////////////////////////////////////////////////////////	
	
	public void createTrips(Population population) throws Exception{
		
		System.out.println("      parsing weges from " + this.wegeInputfile);	
		
		FileReader fr = new FileReader(this.wegeInputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		int weg_counter = 0;
			
		while ((curr_line = br.readLine()) != null) {
			
			weg_counter++;
	
			String[] entries = curr_line.split("\t", -1);
				
			//household number
			String hhnr = entries[0].trim();
			
			//person number (zielpnr)
			String zielpnr = entries[1].trim();
			Id pid = new IdImpl(hhnr.concat(zielpnr));
					
			//wege number
			String wegnr = entries[3].trim();	
			
			//mode
			String mode = entries[80].trim();
			if(mode.equals("1")){mode = "plane";}
			else if(mode.equals("2")){mode = "train";}else if(mode.equals("3")){mode = "postauto";}
			else if(mode.equals("4")){mode = "ship";}else if(mode.equals("5")){mode = "tram";}
			else if(mode.equals("6")){mode = "bus";}else if(mode.equals("7")){mode = "sonstigerOeV";}
			else if(mode.equals("8")){mode = "reisecar";}else if(mode.equals("9")){mode = "car";}
			else if(mode.equals("10")){mode = "truck";}else if(mode.equals("11")){mode = "taxi";}
			else if(mode.equals("12")){mode = "motorcycle";}else if(mode.equals("13")){mode = "mofa";}
			else if(mode.equals("14")){mode = "bicycle";}else if(mode.equals("15")){mode = "walk";}
			else if(mode.equals("16")){mode = "FahrzeugähnlicheGeräte";}else if(mode.equals("17")){mode = "other";}
			else if(mode.equals("-99")){mode = "Pseudoetappe";}
			else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
			
			//start coordinate (round to hectare) - WGS84 (22,23) & CH1903 (24,25)
			Coord start_coord = new CoordImpl(entries[24].trim(),entries[25].trim());
			start_coord.setX(Math.round(start_coord.getX()/100.0)*100);
			start_coord.setY(Math.round(start_coord.getY()/100.0)*100);
			
			//end coordinate (round to hectare) - WGS84 (42,43) & CH1903 (44,45)
			Coord end_coord = new CoordImpl(entries[44].trim(),entries[45].trim());
			end_coord.setX(Math.round(end_coord.getX()/100.0)*100);
			end_coord.setY(Math.round(end_coord.getY()/100.0)*100);
			
			// departure time (min => sec.)
			int departure = Integer.parseInt(entries[5].trim())*60;
			
			// arrival time (min => sec.)
			int arrival = Integer.parseInt(entries[6].trim())*60;
			if(arrival<departure){
				Gbl.errorMsg("This should never happen!  Arrival ("+arrival+") before departure ("+departure+")!- hhnr: " +hhnr+ " zielpnr: "+zielpnr+" wegnr: "+wegnr);
			}
			
			//bee-line distance (km => m)
			double distance = Double.parseDouble(entries[85].trim())*1000.0;
			entries[21] = Double.toString(distance);
			
			//activity type
			String wzweck1 = entries[82].trim();
			String wzweck2 = entries[83].trim();
			
			if(wzweck2.equals("1")){//hinweg
			if(wzweck1.equals("1")){wzweck1 = "Change, change of transport, car park";}
			else if(wzweck1.equals("2")){wzweck1 = "work";}else if(wzweck1.equals("3")){wzweck1 = "education, school";}
			else if(wzweck1.equals("4")){wzweck1 = "shopping";}else if(wzweck1.equals("5")){wzweck1 = "errands and use of services";}
			else if(wzweck1.equals("6")){wzweck1 = "business";}else if(wzweck1.equals("7")){wzweck1 = "dienstfahrt";}
			else if(wzweck1.equals("8")){wzweck1 = "leisure";}else if(wzweck1.equals("9")){wzweck1 = "accompanying (children)";}
			else if(wzweck1.equals("10")){wzweck1 = "accompanying (not children)";}else if(wzweck1.equals("11")){wzweck1 = "return to home";}
			else if(wzweck1.equals("12")){wzweck1 = "other";}
			else if(wzweck1.equals("-99")){wzweck1 = "Pseudoetappe";}
			else Gbl.errorMsg("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
			}else if(wzweck2.equals("2") | wzweck2.equals("3") ){// Nachhauseweg or Weg von zu Hause nach Hause
			wzweck1 = HOME;	}
			else Gbl.errorMsg("This should never happen!  Purpose wzweck2: " +  wzweck2 + " doesn't exist");
					
					
			// creating/getting plan
			PersonImpl person = (PersonImpl) population.getPersons().get(pid);
			Plan plan = person.getSelectedPlan();
			if (plan == null) {
				person.createAndAddPlan(true);
				plan = person.getSelectedPlan();
			}
			
			// adding acts/legs
			if (plan.getPlanElements().size() != 0) { // already lines parsed and added (not first wege)
				ActivityImpl from_act = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
				LegImpl previous_leg = (LegImpl)plan.getPlanElements().get(plan.getPlanElements().size()-2);
				from_act.setEndTime(departure);
				LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
				leg.setDepartureTime(departure);
				leg.setTravelTime(arrival-departure);
				leg.setArrivalTime(arrival);
				//NetworkRoute route = new LinkNetworkRouteImpl(null, null);
				//leg.setRoute(route);
				//route.setDistance(distance);
				//route.setTravelTime(leg.getTravelTime());
				ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(wzweck1,end_coord);
				act.setStartTime(arrival);
			
				// coordinate consistency check
				if ((from_act.getCoord().getX() != start_coord.getX()) || (from_act.getCoord().getY() != start_coord.getY())) {
					 //Gbl.errorMsg("This should never happen!   pid=" + person.getId() + ": previous destination not equal to the current origin (dist=" + ((CoordImpl) from_act.getCoord()).calcDistance(start_coord) + ")");
						coord_err_pids.add(pid);
				}
				
				// time consistency check
				if (previous_leg.getArrivalTime() > leg.getDepartureTime()) {
					 //Gbl.errorMsg("This should never happen!   pid=" + person.getId() + ": activity end time "+ leg.getDepartureTime() + " greater than start time " + previous_leg.getArrivalTime());
						time_err_pids.add(pid);
				}
				
				
				
			}
			else {//first trip (from home)
				ActivityImpl homeAct = ((PlanImpl) plan).createAndAddActivity(HOME,start_coord);
				homeAct.setEndTime(departure);
				LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
				leg.setDepartureTime(departure);
				leg.setTravelTime(arrival-departure);
				leg.setArrivalTime(arrival);
				ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(wzweck1,end_coord);
				act.setStartTime(arrival);
			}
						
		}//end while
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # weges parsed = " + weg_counter  );
		System.out.println();
		
	}
	
//////////////////////////////////////////////////////////////////////

	private final void removePlans(final Population population, final Set<Id> ids) {
		for (Id id : ids) {
			Person p = population.getPersons().remove(id);
			if (p == null) { Gbl.errorMsg("pid="+id+": id not found in the plans DB!"); }
		}
	}


	
//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithoutActivities(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			if(person.getSelectedPlan()==null){
				ids.add(person.getId());}
		}
		return ids;
	}

	
//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyNonHomeBasedPlans(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			if (!last.getType().equals(HOME)) { ids.add(p.getId()); }
		}
		return ids;
	}

//////////////////////////////////////////////////////////////////////	
	
	private final Set<Id> identifyPlansWithNegCoords(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord().getX()<0) || (act.getCoord().getY()<0)) { ids.add(person.getId()); }
				}
			}
		}
		return ids;
	}	
	
//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithTooLongWalkTrips(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if(person.getId().toString().equals("1000101")){
					System.out.println(person.getId());
					System.out.println(leg.getMode());
					}
					if ((leg.getMode().equals(TransportMode.walk))&&(leg.getRoute().getDistance()>10000.0)) {ids.add(person.getId()); }
				}
			}
		}
		return ids;
	}

//////////////////////////////////////////////////////////////////////	

//////////////////////////////////////////////////////////////////////
//run method
//////////////////////////////////////////////////////////////////////

	public void run(Population population) throws Exception{
		
	createPopulation(population);	
	createTrips(population);
		
	
	System.out.println("################################################################################# \n " +
			   		   "Writing complete population xml file \n" +
					   "#################################################################################");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/completePopulation.xml");
	System.out.println("  done.");
	
	System.out.println("################################################################################# \n " +
					   "Writing complete population's attributes xml file \n" +
			   		   "#################################################################################");	
	
	ObjectAttributesXmlWriter population_axmlw = new ObjectAttributesXmlWriter(population_attributes);
	population_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
	population_axmlw.writeFile("./output/MicroCensus2010/completePopulationAttributes.xml");
	System.out.println("  done.");
	
	
	System.out.println("################################################################################# \n " +
						"Started filtering population \n" +
	   		   			"#################################################################################");	

	System.out.println("      Original population size: " +population.getPersons().size() + "\n");
//////////////////////////////////////////////////////////////////////
	// Filtering coordinates inconsistencies
	System.out.println("      Filter1 - removing persons with coord inconsistencies...");
	if(coord_err_pids.size()>0){
	this.removePlans(population, this.coord_err_pids);
	System.out.println("      done.");
	System.out.println("      Filter1 - Total persons removed: " + this.coord_err_pids.size());
	System.out.println("      Filter1 - Remaining population size: " + population.getPersons().size());
	System.out.println("      Filter1 - Writing population without coord. inconsistencies xml file \n");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/PopulationAfterFilter1.xml");
	System.out.println("  done.");
	
	}else{System.out.println("      Filter1 - NO PEOPLE WITH COORD INCONSISTENCIES \n");} 
	
//////////////////////////////////////////////////////////////////////
	// Filtering time inconsistencies
	System.out.println("      Filter2 - removing persons with time inconsistencies...");
	if(time_err_pids.size()>0){
	this.removePlans(population, this.time_err_pids);
	System.out.println("      done.");
	System.out.println("      Filter2 - Total persons removed: " + this.time_err_pids.size());
	System.out.println("      Filter2 - Remaining population size: " + population.getPersons().size());
	System.out.println("      Filter2 - Writing population without time  inconsistencies xml file \n");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/PopulationAfterFilter2.xml");
	System.out.println("  done.");
	
	}else{System.out.println("      Filter2 - NO PEOPLE WITH TIME INCONSISTENCIES \n");}
	
//////////////////////////////////////////////////////////////////////
	//  Filtering  persons without activities
	System.out.println("      Filter3 - removing persons with no activities...");
	Set<Id> no_act_pids = identifyPlansWithoutActivities(population);
	if(no_act_pids.size()>0){
	this.removePlans(population, no_act_pids);
	System.out.println("      done.");
	System.out.println("      Filter3 - Total persons removed: " + no_act_pids.size());
	System.out.println("      Filter3 - Remaining population size: " + population.getPersons().size());
	System.out.println("      Filter3 - Writing population without people with no activities xml file \n");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/PopulationAfterFilter3.xml");
	System.out.println("  done.");
	
	}else{System.out.println("      Filter3 - NO PEOPLE WITHOUT ACTIVITIES FOUND \n");}

//////////////////////////////////////////////////////////////////////
	// Filtering non home base trips
	System.out.println("      Filter4 - removing persons with non-home based trips...");
	Set<Id> nhbt_pids = identifyNonHomeBasedPlans(population);
	if(nhbt_pids.size()>0){
	this.removePlans(population, nhbt_pids);
	System.out.println("      done.");
	System.out.println("      Filter4 - Total persons removed: " + nhbt_pids.size());
	System.out.println("      Filter4 - Remaining population size: " + population.getPersons().size());
	System.out.println("      Filter4 - Writing population without non home-based trips xml file \n");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/PopulationAfterFilter4.xml");
	System.out.println("  done.");
	
	}else{System.out.println("      Filter4 - NO NON HOME-BASED TRIPS FOUND \n");}

//////////////////////////////////////////////////////////////////////	
	// Filtering people with plans with negative coords
	System.out.println("      Filter5 - removing persons with plans with negative coords...");
	Set<Id> negp_pids = identifyPlansWithNegCoords(population);
	if(negp_pids.size()>0){
	this.removePlans(population, negp_pids);
	System.out.println("      done.");
	System.out.println("      Filter5 - Total persons removed: " + negp_pids.size());
	System.out.println("      Filter5 - Remaining population size: " + population.getPersons().size());
	System.out.println("      Filter5 - Writing population without people with plans with negative coords xml file \n");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/PopulationAfterFilter5.xml");
	System.out.println("  done.");
	
	}else{System.out.println("      Filter5 - NO PEOPLE WITH NEGATIVE COORDS FOUND \n");}

//////////////////////////////////////////////////////////////////////	
	// Filtering people with too long walk trips
/*	System.out.println("      Filter6 - removing persons with plans with too long walk trips...");
	Set<Id> longw_pids = identifyPlansWithTooLongWalkTrips(population);
	if(longw_pids.size()>0){
	this.removePlans(population, longw_pids);
	System.out.println("      done.");
	System.out.println("      Filter6 - Total persons removed: " + longw_pids.size());
	System.out.println("      Filter6 - Remaining population size: " + population.getPersons().size());
	System.out.println("      Filter6 - Writing population without people with too long walk trips xml file \n");	
	new PopulationWriter(population, null).write("./output/MicroCensus2010/PopulationAfterFilter6.xml");
	System.out.println("  done.");
	
	}else{System.out.println("      Filter6 - NO PEOPLE WITH TOO LONG WALK TRIPS \n");}
*/	
	
	System.out.println("################################################################################# \n " +
					"Finished filtering population. Las population size = "+ population.getPersons().size() + "\n" +
					"#################################################################################");	

//////////////////////////////////////////////////////////////////////		
	
	}	
	
}
