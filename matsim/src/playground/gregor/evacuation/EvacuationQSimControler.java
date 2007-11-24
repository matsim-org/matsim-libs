/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationQSimControler.java
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

package playground.gregor.evacuation;


import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;
import org.xml.sax.SAXException;



/**
 * @author glaemmel
 *
 */
public class EvacuationQSimControler extends Controler {



	@Override
	protected Plans loadPopulation() {
		String plansFile = Gbl.getConfig().findParam("plans", "inputEvacuationPlansPrefix")
		+ Time.strFromSec((int)Gbl.parseTime(Gbl.getConfig().findParam("simulation", "evacuationTime")))
		+ "." + Gbl.getConfig().findParam("plans", "inputEvacuationPlansSuffix");

		Plans population = new Plans(Plans.NO_STREAMING);

		printNote("", "  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFile);
		population.printPlansCount();
		printNote("", "  done");

		return population;
	}

	@Override
	protected void loadData() {
		super.loadData();
		HashMap<IdI, EvacuationAreaLink> desasterAreaLinks = new HashMap<IdI, EvacuationAreaLink>();
		EvacuationNetFileReader enfr = new EvacuationNetFileReader(desasterAreaLinks);
		try {
			enfr.readFile("networks/desaster_area.xml.gz");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		EvacuationPlansGenerator epg = new EvacuationPlansGenerator();
		epg.generatePlans(this.population,this.network,desasterAreaLinks);
	}





	public static void main(String[] args) {
		final Controler controler = new EvacuationQSimControler();

		Runtime run = Runtime.getRuntime();
		run.addShutdownHook( new Thread()
				{
					@Override
					public void run()
					{
						controler.shutdown(true);
					}
				} );

		controler.run(args);
		System.exit(0);
	}
}
