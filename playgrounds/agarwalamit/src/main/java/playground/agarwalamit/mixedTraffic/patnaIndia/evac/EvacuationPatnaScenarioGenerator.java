/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.grips.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.old.DefaultRoutingModules;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.LoadMyScenarios;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author amit
 */

public class EvacuationPatnaScenarioGenerator {

	public static void main(String[] args) {
		new EvacuationPatnaScenarioGenerator().run();
	}

	private void run (){
		Collection <String> mainModes = Arrays.asList("car","motorbike","bike");

		String dir = "../../../repos/runs-svn/patnaIndia/";

		String networkFile = dir+"/inputs/networkUniModal.xml";
		String outNetworkFile = dir+"/run105/input/evac_network.xml.gz";

		String popFile = dir+"/inputs/SelectedPlansOnly.xml";
		String outPopFile = dir+"/run105/input/evac_plans.xml.gz";

		String areShapeFile = dir+"/run105/input/area_epsg24345.shp";

		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(popFile, networkFile);

		Id<Link> safeLinkId = Id.createLinkId("safeLink_Patna");

		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(areShapeFile);
		Geometry evavcuationArea = (Geometry) features.iterator().next().getDefaultGeometry();

		// will create a network connecting with safe node.
		EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(sc, evavcuationArea, safeLinkId);
		net.run();

		new NetworkWriter(sc.getNetwork()).write(outNetworkFile);

		// population, (home - evac)
		Scenario scOut = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population popOut = scOut.getPopulation();
		PopulationFactory popFact = popOut.getFactory();

		for(Person p : sc.getPopulation().getPersons().values()){

			PlanElement actPe = p.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
			Activity home = popFact.createActivityFromLinkId(((Activity)actPe).getType(), ((Activity)actPe).getLinkId());
			//Activity home = popFact.createActivityFromCoord(((Activity)actPe).getType(), ((Activity)actPe).getCoord());

			//check if the person is in the area shape, if not leave them out
			if(! evavcuationArea.contains(MGC.coord2Point(((Activity)actPe).getCoord()))){
				continue;
			}
			
			// also exclude any home activity starting on link which is not included in output network
			if(! sc.getNetwork().getLinks().containsKey(home.getLinkId())){
				continue;
			}

			Person pOut = popFact.createPerson(p.getId());
			Plan planOut = popFact.createPlan();
			pOut.addPlan(planOut);

			planOut.addActivity(home);
			home.setEndTime(9*3600);

			PlanElement legPe = p.getSelectedPlan().getPlanElements().get(1);
			Leg leg = popFact.createLeg(((Leg)legPe).getMode());
			planOut.addLeg(leg);

			Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkId);
			planOut.addActivity(evacAct);

			if(mainModes.contains(leg.getMode())){
				ModeRouteFactory routeFactory = new ModeRouteFactory();
				routeFactory.setRouteFactory(leg.getMode(), new LinkNetworkRouteFactory());
				
				TripRouter router = new TripRouter();
				router.setRoutingModule(leg.getMode(), DefaultRoutingModules.createNetworkRouter(leg.getMode(), popFact, sc.getNetwork(), new Dijkstra(sc.getNetwork(), new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()) , new FreeSpeedTravelTime())));
				List<? extends PlanElement> routeInfo = router.calcRoute(leg.getMode(), new ActivityWrapperFacility(home), new ActivityWrapperFacility(evacAct), home.getEndTime(), pOut);
				
				leg.setRoute(((Leg)routeInfo.get(0)).getRoute());
				leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());
				
//				router.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), scenario.getNetwork(), new FreeSpeedTravelTime()));
//				RoutingModule routinModule = DefaultRoutingModules.createNetworkRouter(leg.getMode(), popFact, sc.getNetwork(), routeAlgo)
//				leg.setRoute(route);
			} else {
				ModeRouteFactory routeFactory = new ModeRouteFactory();
				routeFactory.setRouteFactory(leg.getMode(), new GenericRouteFactory());
				
				Route route = routeFactory.createRoute(leg.getMode(), home.getLinkId(), evacAct.getLinkId());
				leg.setRoute(route);
			}
			popOut.addPerson(pOut);
		}

		new PopulationWriter(popOut).write(outPopFile);		
	}
}
