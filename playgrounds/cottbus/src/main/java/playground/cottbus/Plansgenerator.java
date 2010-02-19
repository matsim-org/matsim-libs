/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator
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

package playground.cottbus;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * @author	rschneid-btu
 * [based on same file from dgrether]
 * generates an amount of agents per commodity (see addCommodity for further details)
 */
public class Plansgenerator {
	// =====================================================
	private static final String chosenScenario = "portland";
	// =====================================================
	// ======== choose: denver / portland / cottbus ========
	// =====================================================

	private static final String networkFilename = "./input/"+chosenScenario+"/network.xml";

	private static final String plansOut = "./input/"+chosenScenario+"/plans.xml";

	private Scenario scenario;
	private Network network;
	private Population plans;

	private void init() {
		this.scenario = new ScenarioImpl();
		this.network = this.scenario.getNetwork();
		new MatsimNetworkReader(this.scenario).readFile(networkFilename);
	}

	private void createPlans() throws Exception {
		init();
		this.plans = this.scenario.getPopulation();
		final int HOME_END_TIME = 6 * 3600; // time to start

		if(chosenScenario.equals("cottbus")) {
			createCottbusFirst(HOME_END_TIME);
		} else
		if(chosenScenario.equals("denver")) {
			createDenverStraight(HOME_END_TIME);
		} else
		if(chosenScenario.equals("portland")) {
			createPortland(HOME_END_TIME);
		}

		new PopulationWriter(this.plans, this.network).writeFile(plansOut);
	}

	/**
	 * generates agents for portland/oregon scenario
	 * @param HOME_END_TIME
	 */
	private void createPortland(final int HOME_END_TIME) {
		int currentId = 1;
		int duration = (int)(0.5 * 3600); // seconds
		final int DEFAULT_CARS_PER_HOUR_PER_LANE = 1000;

		// #1 green220
		currentId = addCommodity(
				"8","2",HOME_END_TIME,duration,(int)(0.22*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"4 3 2",currentId);
		// #2 green700
		currentId = addCommodity(
				"145","67",HOME_END_TIME,duration,(int)(0.7*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"1 7 13 19",currentId);
		// #3 green100
		currentId = addCommodity(
				"8","2",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"4 3 2",currentId);
		// #4 purple190
		currentId = addCommodity(
				"2","13",HOME_END_TIME,duration,(int)(0.19*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"1 7 8",currentId);
		// #5 rosa250
		currentId = addCommodity(
				"13","158",HOME_END_TIME,duration,(int)(0.25*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"9 10 4",currentId);
		// #6 pink100
		currentId = addCommodity(
				"78","130",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"20 14 13",currentId);
		// #7 lightblue180
		currentId = addCommodity(
				"92","122",HOME_END_TIME,duration,(int)(0.18*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"4 3 2 1",currentId);
		// #8 blue150
		currentId = addCommodity(
				"133","24",HOME_END_TIME,duration,(int)(0.15*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"19 20 21 22 16 15",currentId);
		// #9 darkblue250
		currentId = addCommodity(
				"33","37",HOME_END_TIME,duration,(int)(0.25*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"21 22",currentId);
		// #10 lightbrown700
		currentId = addCommodity(
				"125","17",HOME_END_TIME,duration,(int)(0.7*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"7 8 9 10",currentId);
		// #11 brown340
		currentId = addCommodity(
				"78","122",HOME_END_TIME,duration,(int)(0.34*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"20 14 8 2 1",currentId);
		// #12 red700
		currentId = addCommodity(
				"98","158",HOME_END_TIME,duration,(int)(0.7*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"22 16 10 4",currentId);
		// #13 yellow240
		currentId = addCommodity(
				"28","130",HOME_END_TIME,duration,(int)(0.24*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"16 15 14 13",currentId);
		
	}
	
	/**
	 * generates agents for cottbus scenario
	 * @param HOME_END_TIME
	 */
	private void createCottbusFirst(final int HOME_END_TIME) {
		int currentId = 1;
		int duration = (int)(0.5 * 3600); // seconds
		final int DEFAULT_CARS_PER_HOUR_PER_LANE = 4*2000;

		// #1 uni zu stadion
		currentId = addCommodity(
				"701","324",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"26 27 28 29 30 32",currentId);
		// #2 stadion zu uni
		currentId = addCommodity(
				"192","703",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"32 30 29 28 27 26",currentId);
		// #3 bahnhof zu cottbus nord
		currentId = addCommodity(
				"662","602",HOME_END_TIME,duration,(int)(0.3*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"21 22 23 37 24 25 26 11",currentId);
		// #4 cottbus nord zu bahnhof
		currentId = addCommodity(
				"604","664",HOME_END_TIME,duration,(int)(0.3*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"11 26 25 24 37 23 22 21",currentId);
		// #5 strasse der jugend richtung peitz
		currentId = addCommodity(
				"652","622",HOME_END_TIME,duration,(int)(0.3*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"20 51 19 50 18 17 16 15",currentId);
		// #6 peitz zu bahnhof
		currentId = addCommodity(
				"624","673",HOME_END_TIME,duration,(int)(0.3*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"15 16 17 18 50 19 51 20 21",currentId);
		// #7 karl-liebknecht zu max bahr
		currentId = addCommodity(
				"681","631",HOME_END_TIME,duration,(int)(0.3*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"23 35 34 33 32 31 18",currentId);
		// #8 max bahr zu karl-liebknecht
		currentId = addCommodity(
				"633","683",HOME_END_TIME,duration,(int)(0.3*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"18 31 32 33 34 35 23",currentId);
		// #9 str der jugend zu TKC
		currentId = addCommodity(
				"652","725",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"20 35 34 33 52 53 36 29 28 54 55 14 38",currentId);
		// #10 TKC zu str der jugend
		currentId = addCommodity(
				"727","654",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"38 14 55 54 28 29 36 53 52 33 34 35 20",currentId);

		// #11 post zu dissenchener
		currentId = addCommodity(
				"691","311",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"24 36 29 30 31",currentId);
		// #12 branitz zu peitz
		currentId = addCommodity(
				"642","622",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"19 50 18 17 16 15",currentId);
		// #13 peitz zu branitz
		currentId = addCommodity(
				"624","644",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"15 16 17 18 50 19",currentId);
		// #14 branitz zu sielow
		currentId = addCommodity(
				"642","602",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"19 50 18 17 16 15 39 14 38 13 12 11",currentId);
		// #15 sielow zu branitz
		currentId = addCommodity(
				"604","644",HOME_END_TIME,duration,(int)(0.1*DEFAULT_CARS_PER_HOUR_PER_LANE),
				"11 12 13 38 14 39 15 16 17 18 50 19",currentId);


	}


	/**
	 * generates agents for denver (straight) scenario
	 * @param HOME_END_TIME
	 */
	private void createDenverStraight(final int HOME_END_TIME) {
		int currentId = 1;
		int duration = (int)(0.5 * 3600); // seconds
		final int DEFAULT_CARS_PER_HOUR_PER_LANE = 500;

		// horizontal (east-west)
		currentId = addCommodity(
				"125","127",HOME_END_TIME,duration,4*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"7 8 9 10 11 12",currentId);

		currentId = addCommodity(
				"133","135",HOME_END_TIME,duration,4*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"19 20 21 22 23 24",currentId);

		currentId = addCommodity(
				"124","122",HOME_END_TIME,duration,2*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"6 5 4 3 2 1",currentId);

		currentId = addCommodity(
				"132","130",HOME_END_TIME,duration,3*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"18 17 16 15 14 13",currentId);

		currentId = addCommodity(
				"144","142",HOME_END_TIME,duration,3*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"36 35 34 33 32 31",currentId);

		// bus lane (east-west)
		currentId = addCommodity(
				"137","139",HOME_END_TIME,duration,1*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"25 26 27 28 29 30",currentId);
		currentId = addCommodity(
				"140","138",HOME_END_TIME,duration,1*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"30 29 28 27 26 25",currentId);

		// vertical (north-south)
		currentId = addCommodity(
				"145","147",HOME_END_TIME,duration,2*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"1 7 13 19 25 31",currentId);

		currentId = addCommodity(
				"153","155",HOME_END_TIME,duration,3*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"3 9 15 21 27 33",currentId);

		currentId = addCommodity(
				"161","163",HOME_END_TIME,duration,3*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"5 11 17 23 29 35",currentId);

		currentId = addCommodity(
				"152","150",HOME_END_TIME,duration,3*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"32 26 20 14 8 2",currentId);

		currentId = addCommodity(
				"160","158",HOME_END_TIME,duration,3*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"34 28 22 16 10 4",currentId);

		currentId = addCommodity(
				"168","166",HOME_END_TIME,duration,2*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"36 30 24 18 12 6",currentId);

	}

	/**
	 * only for first testing used, probably used later
	 * @param HOME_END_TIME
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void createDenverIndividual(final int HOME_END_TIME) {
		int currentId = 1;
		int duration = (int)(0.5 * 3600);
		final int DEFAULT_CARS_PER_HOUR_PER_LANE = 500;

		currentId = addCommodity(
				"168","166",HOME_END_TIME,duration,4*DEFAULT_CARS_PER_HOUR_PER_LANE,
				"36 30 24 18 12 6",currentId);

	}

	/**
	 * customized algorithm: automatically fills routes with agents, main parts of adding agents by dgrether
	 * start time of each agent is randomized, but between start_time and (start_time + duration)
	 * @param HOME_LINK
	 * @param TARGET_LINK
	 * @param START_TIME
	 * @param DURATION
	 * @param CARS_PER_HOUR
	 * @param ROUTE
	 * @param CURRENT_ID
	 * @return next ID to continue
	 */
	private int addCommodity(final String HOME_LINK, final String TARGET_LINK, final int START_TIME, final int DURATION, final int CARS_PER_HOUR, final String ROUTE, int CURRENT_ID){
		int homeEndtime = 0;
		final Link start = network.getLinks().get(new IdImpl(HOME_LINK));
		final Link target = network.getLinks().get(new IdImpl(TARGET_LINK));
		final int visPlaces = 2000; //better visualization of start and target
		final Coord startCoord = start.getToNode().getCoord();
		final Coord targetCoord = target.getFromNode().getCoord();
		final Coord homeCoord = new CoordImpl(startCoord.getX()-visPlaces,startCoord.getY()+visPlaces);
		final Coord workCoord = new CoordImpl(targetCoord.getX()+visPlaces,targetCoord.getY()-visPlaces);

		final int AMOUNT_OF_CARS = (int)((CARS_PER_HOUR * DURATION * 1.0) / 3600);
//		System.out.println("start: "+start.getId()+" zielLink: "+target.getId()+" cars: "+AMOUNT_OF_CARS+ "c/h: "+CARS_PER_HOUR+" dur: "+((DURATION*1.0)/3600));
		final int MAX_ID = CURRENT_ID+1 + AMOUNT_OF_CARS;

		for (int i = CURRENT_ID+1; i <= MAX_ID; i++) {
			homeEndtime = START_TIME;

			PersonImpl p = new PersonImpl(new IdImpl(i));
			PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
			p.addPlan(plan);
			//home
			homeEndtime += Math.floor(Math.random() * DURATION); //0.05 * 60;
			ActivityImpl a = plan.createAndAddActivity("h", homeCoord);
			a.setLinkId(start.getId());
			a.setEndTime(homeEndtime);
			//leg to work
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = new LinkNetworkRouteImpl(start.getId(), target.getId(), this.network);
			route.setLinkIds(start.getId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(network, ROUTE))), target.getId());
			leg.setRoute(route);
			//work
			a = plan.createAndAddActivity("w", workCoord);
			a.setLinkId(target.getId());

			this.plans.addPerson(p);

		}

		return MAX_ID;
	}


	public static void main(final String[] args) {
		try {
			new Plansgenerator().createPlans();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
