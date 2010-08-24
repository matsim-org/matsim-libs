/* *********************************************************************** *
 * project: org.matsim.*
 * PersonInitDemandSummaryTable.java
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

package playground.ciarif.models.subtours;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.population.PlanImpl;

public class PersonInitDemandSummaryTable {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private List<PersonSubtour> personSubtours;
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonInitDemandSummaryTable(String outfile, List<PersonSubtour> personSubtours) {
		super();
		this.personSubtours = personSubtours;
		try {
			fw = new FileWriter(outfile);
			System.out.println(outfile);
			out = new BufferedWriter(fw);
			out.write("pid \t subtour_id \t purpose\t prev_subtour\t mode \t prev_mode\t start_x \t start_y \t start_udeg\t distance\t trips\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	private void writePersonSubtour (PersonSubtour personSubtour) {
		try {
						
			
			
			Iterator<Subtour> subtour_it =  personSubtour.getSubtours().iterator();
				while (subtour_it.hasNext()) {
					Subtour subtour = subtour_it.next();
					out.write(personSubtour.getPerson_id().toString()+"\t");
					out.write(subtour.getId()+ "\t");
					out.write(subtour.getPurpose()+"\t");
					out.write(subtour.getPrev_subtour()+"\t");
					out.write(subtour.getMode()+"\t");
					out.write(subtour.getPrev_mode()+"\t");
					out.write(subtour.getStart_coord().getX()+"\t");
					out.write(subtour.getStart_coord().getY()+"\t");
					out.write(subtour.getStart_udeg()+"\t");
					out.write(subtour.getDistance()+"\t");
					out.write(subtour.getNodes().size()-1 + "\n");
					
					//System.out.println("Anzahl"+subtour.getPurpose());
					
					
					out.flush();
				}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public void write() {
		try {
			Iterator<PersonSubtour> ps_it = this.personSubtours.iterator();
			while (ps_it.hasNext()) {
				PersonSubtour personSubtour = ps_it.next();
				writePersonSubtour (personSubtour);
			}
			
			out.flush();
			//this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}

	public void run(PlanImpl plan) {
		
	}
}
