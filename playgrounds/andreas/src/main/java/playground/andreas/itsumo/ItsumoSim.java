/* *********************************************************************** *
 * project: org.matsim.*
 * ItsumoSim.java
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

package playground.andreas.itsumo;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.io.IOUtils;

public class ItsumoSim extends ExternalMobsim {

	protected static final String CONFIG_MODULE = "itsumo";
	private ControlerIO controlerio;
	private Integer iteration;

	public ItsumoSim(final Population population, final Network network, final EventsManager events) {
		super(population, network, events);
		System.out.println("\n##################################################################################################\n" +
				"#   REMINDER - Header in writeItsumoConfig has to be changed\n" +
				"#              according to the itsumo scenario description file." +
		"\n##################################################################################################\n");
	}

	@Override
	public void run() {
		// ONLY reason why this needs to be overridden is because of the different events file name !!
		// Since it new exists, writeItsumoConfig is different from writeConfig.

		String iterationPlansFile = controlerIO.getIterationFilename(this.iteration, this.plansFileName);
//		String iterationEventsFile = Controler.getIterationFilename(this.eventsFileName);
		String iterationEventsFile = "./drivers.txt" ;
		String iterationConfigFile = controlerIO.getIterationFilename(this.iteration, this.configFileName);

		try {
			writeConfig( iterationPlansFile, iterationEventsFile, "output/config.xml" ) ;
			writeItsumoConfig( iterationConfigFile ) ;
			runExe(iterationConfigFile);
			readEvents(iterationEventsFile);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void writeItsumoConfig(String outFileName ) throws FileNotFoundException, IOException {
		System.out.println("writing config AND plans into config file for itsumo mobsim at " + (new Date()));

		BufferedWriter out = null ;
		try {
			out = IOUtils.getBufferedWriter( outFileName ) ;

			// aneumann
			out.write("<config>"); out.newLine();
			out.write(" <file>" + Gbl.getConfig().getParam(ItsumoSim.CONFIG_MODULE, "itsumoInputNetworkFile") + "</file>"); out.newLine();
			out.write(" <steps>3600</steps>"); out.newLine();
			out.write(" <default_deceleration>0</default_deceleration>"); out.newLine();
			out.write(" <sensor_interval>1</sensor_interval>"); out.newLine();
			out.write(" <agent_interval>40</agent_interval>"); out.newLine();
			out.write(" <car_max_speed>3</car_max_speed>"); out.newLine();
			out.write(" <cell_size>5</cell_size>"); out.newLine();
			out.write(" <iteration_length>1</iteration_length>"); out.newLine();

			out.write(" <drivers>"); out.newLine();

			for (Person person : this.population.getPersons().values()) {

				out.write("  <driver>"); out.newLine();

				out.write("   <name>fc</name>"); out.newLine();
				out.write("   <nick>" + person.getId() + "</nick>"); out.newLine();
				out.write("   <number>1</number>"); out.newLine();
				out.write("   <state>ON</state>"); out.newLine();
				out.write("   <debug>ON</debug>"); out.newLine();

				out.write("   <options>"); out.newLine();
				out.write("    <insert_timestep>1</insert_timestep>"); out.newLine();
				out.write("   </options>"); out.newLine();


				Plan plan = person.getSelectedPlan();
				if ( plan==null ) {
					continue ;
				}

				out.write("   <routes>"); out.newLine();

				// act/leg
				for (int jj = 0; jj < plan.getPlanElements().size(); jj++) {
					if (jj % 2 == 0) {
						ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(jj);

						out.write("    <route>"); out.newLine();
						out.write("     <laneset>" + act.getLinkId() + "</laneset>"); out.newLine();
						out.write("    </route>"); out.newLine();

						if ( jj==2 ) {
							break ;  // we write only the first leg for itsumo!
						}
					} else {
						LegImpl leg = (LegImpl)plan.getPlanElements().get(jj);
						// route
						if (leg.getRoute() == null) {
							System.err.println ( " WARNING: Empty route.  Not sure if itsumo can deal with this.  Continuing anyway ... " ) ;
							continue ;
						}
						NetworkRouteWRefs rr = (NetworkRouteWRefs) leg.getRoute();
						for (Id linkId : rr.getLinkIds()) {
							out.write("    <route>"); out.newLine();
							out.write("     <laneset>" + linkId + "</laneset>"); out.newLine();
							out.write("    </route>"); out.newLine();
						}
					}
				}
				out.write("   </routes>"); out.newLine();
				out.write("  </driver>"); out.newLine();
				out.flush() ;
			}
			out.write(" </drivers>"); out.newLine();

			out.write(" <sensors>"); out.newLine();

			out.write("  <sensor>"); out.newLine();
			out.write("   <name>total_stopped_cars_in_network</name>"); out.newLine();
			out.write("   <file>" + this.controlerio.getIterationPath(this.iteration) + "/" + Controler.getIteration() + ".itsumo.total_stopped_cars_in_network.log</file>"); out.newLine();
			out.write("   <state>OFF</state>"); out.newLine();
			out.write("  </sensor>"); out.newLine();

			out.write("  <sensor>"); out.newLine();
			out.write("   <name>stopped_cars_in_lanesets</name>"); out.newLine();
			out.write("   <file>" + this.controlerio.getIterationPath(this.iteration) + "/" + Controler.getIteration() + ".itsumo.stopped_cars_in_lanesets.log</file>"); out.newLine();
			out.write("   <state>OFF</state>"); out.newLine();
			out.write("  </sensor>"); out.newLine();

			out.write(" </sensors>"); out.newLine();

			out.write("</config>"); out.newLine();
			out.flush();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}

	}

	@Override
	public void setControlerIO(ControlerIO controlerIO) {
		this.controlerio = controlerIO;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}


}
