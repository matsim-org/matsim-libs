/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityFrequenciesAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Counter;


/**
 * Creates a table of the visitor frequencies for visitors of a certain shop or leisure facility.
 * @author anhorni
 */

public class ShopLeisureFacilityFrequenciesAnalyzer {



	private Plans plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities =null;

	private final static Logger log = Logger.getLogger(ShopLeisureFacilityFrequenciesAnalyzer.class);


	/**
	 * @param
	 *  - path of the plans file
	 */
	public static void main(String[] args) {

		if (args.length < 1 || args.length > 1 ) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String plansfilePath=args[0];
		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";

		log.info(plansfilePath);

		ShopLeisureFacilityFrequenciesAnalyzer analyzer = new ShopLeisureFacilityFrequenciesAnalyzer();
		analyzer.init(plansfilePath, networkfilePath, facilitiesfilePath);
		analyzer.collectAgents();
		analyzer.writeFacilityFrequencies();
	}

	private void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath) {


		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		this.plans=new Plans(false);
		final PlansReaderI plansReader = new MatsimPlansReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
	}

	private void collectAgents() {
		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			counter.incCounter();

			Plan selectedPlan = person.getSelectedPlan();

			final ArrayList<?> actslegs = selectedPlan.getActsLegs();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Act act = (Act)actslegs.get(j);

				if (act.getType().startsWith("s") || act.getType().startsWith("l")) {
					act.getFacility().addVisitorsPerDay(1);
				}
			}
		}
	}

	private void writeFacilityFrequencies() {

		try {

			final String header="Facility_id\tx\ty\tNumberOfVisitors";

			final BufferedWriter out = IOUtils.getBufferedWriter("./output/facFrequencies.txt");
			out.write(header);
			out.newLine();

			// take all facilities which are not 0. Change to getFac(type)
			Iterator<? extends Facility> iter = this.facilities.iterator();
			while (iter.hasNext()){
				Facility facility = iter.next();

				if (facility.getNumberOfVisitorsPerDay()>0) {
					out.write(facility.getId().toString()+"\t"+ String.valueOf(facility.getCenter().getX())+"\t"+
					String.valueOf(facility.getCenter().getY())+"\t"+String.valueOf(facility.getNumberOfVisitorsPerDay()));
					out.newLine();
				}
				out.flush();
			}
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
