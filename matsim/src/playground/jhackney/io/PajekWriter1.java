/* *********************************************************************** *
 * project: org.matsim.*
 * PajekWriter1.java
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

package playground.jhackney.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;


import playground.jhackney.algorithms.FacilitiesFindScenarioMinMaxCoords;
import playground.jhackney.socialnet.SocialNetEdge;

public class PajekWriter1 {

	private CoordI minCoord;
	private CoordI maxCoord;
	private TreeMap<IdI, Integer> pajekIndex= new TreeMap<IdI, Integer>();
	
	public PajekWriter1(String dir, Facilities facilities){

		//String pjoutdir = Gbl.getConfig().findParam(Gbl.getConfig().SOCNET, Gbl.getConfig().SOCNET_OUT_DIR);
		File pjDir=new File(dir+"/pajek/");
		if(!(pjDir.mkdir())&& !pjDir.exists()){
			Gbl.errorMsg("Cannot create directory "+dir+"/pajek/");
		}
		Gbl.noteMsg(this.getClass(),"","is a dumb writer for UNDIRECTED nets. Replace it with something that iterates through Persons and call it from SocialNetworksTest.");
		FacilitiesFindScenarioMinMaxCoords fff= new FacilitiesFindScenarioMinMaxCoords();
		fff.run(facilities);
		minCoord = fff.getMinCoord();
		maxCoord = fff.getMaxCoord();
		System.out.println(" PW X_Max ="+maxCoord.getX());
		System.out.println(" PW Y_Max ="+maxCoord.getY());
		System.out.println(" PW X_Min ="+minCoord.getX());
		System.out.println(" PW Y_Min ="+minCoord.getY());
		
	}

	public void write(ArrayList links, Plans plans, int iter) {
		BufferedWriter pjout = null;

		// from config
		String pjoutdir = Gbl.getConfig().socnetmodule().getOutDir();
		String pjoutfile = pjoutdir+"pajek/test"+iter+".net";


		try {

			pjout = new BufferedWriter(new FileWriter(pjoutfile));
			System.out.println(" Successfully opened pjoutfile "+pjoutfile);

		} catch (final IOException ex) {
		}

		int numPersons = plans.getPersons().values().size();

		try {
//			System.out.print(" *Vertices " + numPersons + " \n");
			pjout.write("*Vertices " + numPersons);
			pjout.newLine();

			Iterator itPerson = plans.getPersons().values().iterator();
			int iperson = 1;
			while (itPerson.hasNext()) {
				Person p = (Person) itPerson.next();
				final Knowledge know = p.getKnowledge();
				if (know == null) {
					Gbl.errorMsg("Knowledge is not defined!");
				}
				Coord xy = (Coord) ((Act) p.getSelectedPlan().getActsLegs().get(0)).getCoord();
				double x=(xy.getX()-minCoord.getX())/(maxCoord.getX()-minCoord.getX());
				double y=(xy.getY()-minCoord.getY())/(maxCoord.getY()-minCoord.getY());
				pjout.write(iperson + " \"" + p.getId() + "\" "+x +" "+y);
				pjout.newLine();
//				System.out.print(iperson + " " + p.getId() + " ["+xy.getX() +" "+xy.getY()+"]\n");
				pajekIndex.put(p.getId(),iperson);
				iperson++;

			}
			pjout.write("*Edges");
			pjout.newLine();
//			System.out.print("*Edges\n");
			Iterator itLink = links.iterator();
			while (itLink.hasNext()) {
				SocialNetEdge printLink = (SocialNetEdge) itLink.next();
				Person printPerson1 = printLink.person1;
				Person printPerson2 = printLink.person2;

				pjout.write(" " + pajekIndex.get(printPerson1.getId()) + " "+ pajekIndex.get(printPerson2.getId()));
//				pjout.write(" " + printPerson1.getId() + " "+ printPerson2.getId());
				pjout.newLine();
//				System.out.print(" " +iter+" "+printLink.getLinkId()+" "+ printPerson1.getId() + " "
//				+ printPerson2.getId() + " "
//				+ printLink.getTimeLastUsed()+"\n");
			}

		} catch (IOException ex1) {
		}

		try {
			pjout.close();
			System.out.println(" Successfully closed pjoutfile "+pjoutfile);
		} catch (IOException ex2) {
		}
		//}
	}
}
