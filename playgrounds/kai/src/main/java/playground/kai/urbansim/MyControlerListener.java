/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.kai.urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 *
 * @author nagel
 *
 */
public class MyControlerListener implements /*IterationEndsListener,*/ ShutdownListener {
	private static final Logger log = Logger.getLogger(MyControlerListener.class);

	ActivityFacilitiesImpl zones ;

	public MyControlerListener( ActivityFacilitiesImpl zones ) {
		this.zones = zones ;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." ) ;

		// get the calling services:
		MatsimServices controler = event.getServices() ;

		TravelTime ttc = controler.getLinkTravelTimes();
		LeastCostPathTree st = new LeastCostPathTree(ttc, new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, controler.getConfig().planCalcScore() ).createTravelDisutility(ttc));

        Network network = (Network) controler.getScenario().getNetwork();
		double dpTime = 8.*3600 ;

		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(Matsim4Urbansim.PATH_TO_OPUS_MATSIM+"tmp/travel_data.csv");

			log.info("Computing and writing travel_data" ) ;
//			log.warn("Can't feed floats to urbansim; am thus feeding ints for the ttime.") ;
			// solved 3dec08 by travis

			writer.write ( "from_zone_id:i4,to_zone_id:i4,single_vehicle_to_work_travel_cost:f4" ) ; writer.newLine();

			System.out.println("|--------------------------------------------------------------------------------------------------|") ;
			long cnt = 0 ; long percentDone = 0 ;
			for ( ActivityFacility fromZone : zones.getFacilities().values() ) {
				if ( (int) (100.*cnt/zones.getFacilities().size()) > percentDone ) {
					percentDone++ ; System.out.print('.') ;
				}
				cnt++ ;
				Coord coord = fromZone.getCoord() ;
				assert( coord != null ) ;
				final Coord coord1 = coord;
				Node fromNode = NetworkUtils.getNearestNode(network,coord1) ;
				assert( fromNode != null ) ;
				st.calculate(network, fromNode, dpTime) ;
				for ( ActivityFacility toZone : zones.getFacilities().values() ) {
					Coord toCoord = toZone.getCoord() ;
					final Coord coord2 = toCoord;
					Node toNode = NetworkUtils.getNearestNode(network,coord2) ;
					double arrTime = st.getTree().get(toNode.getId()).getTime();
					double ttime = arrTime - dpTime ;
					writer.write ( fromZone.getId().toString()
							+ "," + toZone.getId().toString()
							+ "," + ttime ) ;
					writer.newLine();
				}
			}
			writer.close();
			System.out.println(" ... done") ;
			log.info("... done with writing travel_data" ) ;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("... ... done with notifyShutdown.") ;
	}

//	public void notifyIterationEnds(IterationEndsEvent event) {
//		log.info("Entering notifyIterationEnds ..." ) ;
//
//		// get the calling services:
//		Controler services = event.getServices() ;
//
//		// load first plans file
//		log.info("Reading 0.it plans file ...");
//		String firstPlanPath = Controler.getIterationFilename("plans.xml.gz", 0) ;
//		Population population = new Population(Population.NO_STREAMING);
//		PopulationReader plansReader = new MatsimPopulationReader(population);
//		plansReader.readFile(firstPlanPath);
//		population.printPlansCount();
//		log.info("... done reading 0.it plans file") ;
//
//		log.info("Feeding 0.it plans into planComparison ...") ;
//		PlanComparison result = new PlanComparison(population.getPersons().keySet().size());
//		for (Id id : population.getPersons().keySet()) {
//			Plan plan = population.getPerson(id).getSelectedPlan();
//			Act act = (Act) plan.getIteratorAct().next();
//			result.addFirstPlansData(id, plan.getScore(), act);
//		}
//		log.info("... done feeding 0.it plans into planComparison.") ;
//
//		log.info("Getting second plans set from services and feeding it into planComparison ...") ;
//		population = services.getPopulation();
//		for (Id id : population.getPersons().keySet()) {
//			Plan plan = population.getPerson(id).getSelectedPlan();
//			result.addSecondPlansData(id, plan.getScore());
//		}
//		log.info("... done with second plan set.") ;
//
//		log.info("Writing result to file ...") ;
//		String outpath = Controler.getIterationFilename("comparison_to_0.txt") ;
//		new PlanComparisonFileWriter(outpath).write(result);
//		log.info("... results written to: " + outpath);
//
//	}

}
