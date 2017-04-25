/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author  jbischoff
 *
 */
public class WOBPlansModifier {
public static void main(String[] args) throws IOException {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw079.output_plansNoPTRoutes.xml.gz");
	Geometry geometry = ScenarioPreparator.readShapeFileAndExtractGeometry("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/onezone.shp");
	new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/vw_rufbus/av_simulation/scenario/networkpt-feb.xml.gz");
	Network net = (Network) scenario.getNetwork();
	Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	List<String> starts = new ArrayList<>();
	List<String> ends = new ArrayList<>();
	int z = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){
		z++;
		if (z % 20000 == 0){System.out.println(z);}
		Plan plan = p.getSelectedPlan();
		{
			
			boolean previousActInArea = false;
			Coord previousCoord = null;
			double previousEndtime  = Time.UNDEFINED_TIME;
			for (int i = 0;i < plan.getPlanElements().size();i = i+2){
			if (i == 0){
				Activity act0 = (Activity) plan.getPlanElements().get(0);
				if (geometry.contains(MGC.coord2Point(act0.getCoord()))) {
					previousActInArea = true;
				previousCoord = act0.getCoord();
				previousEndtime = act0.getEndTime();
				}
				else break;
			} else {
				Activity currentAct = (Activity) plan.getPlanElements().get(i);
				boolean currentActInArea = geometry.contains(MGC.coord2Point(currentAct.getCoord()));
				
				if (!currentActInArea) break;
				Coord currentCoord = currentAct.getCoord();
				if (previousActInArea && currentActInArea){
					Leg leg = (Leg) plan.getPlanElements().get(i-1);
					if (leg.getMode().equals(TransportMode.car)||leg.getMode().equals(TransportMode.pt))
{
						
						leg.setMode(TaxiModule.TAXI_MODE);
						Id<Link> start = null;
						Id<Link> end = null;
						if (leg.getRoute()!=null){
						start = leg.getRoute().getStartLinkId();
						end = leg.getRoute().getEndLinkId();
						}
						else {
							final Coord coord = previousCoord;
							start = NetworkUtils.getNearestLinkExactly(net,coord).getId();
							final Coord coord1 = currentCoord;
							end = NetworkUtils.getNearestLinkExactly(net,coord1).getId();
						}
						
						starts.add(previousCoord.getX()+";"+previousCoord.getY()+";"+Time.writeTime(leg.getDepartureTime()));
						ends.add(currentCoord.getX()+";"+currentCoord.getY()+";"+Time.writeTime(currentAct.getStartTime()));
						leg.setRoute(new GenericRouteImpl(start, end));
					}
				}
				previousActInArea = currentActInArea;
				previousCoord = currentCoord;
				previousEndtime = currentAct.getStartTime();
			}	
			}
			Person p2 = pop2.getFactory().createPerson(p.getId());
			p2.addPlan(plan);
			pop2.addPerson(p2);	
		}
	}
	new PopulationWriter(pop2).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/nopt/vw079.taxiplans.xml.gz");
	WobPlansFilter.replaceCarLegsByTeleport(pop2);
	new PopulationWriter(pop2).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/nopt/vw079.taxiplans_noCars.xml.gz");
	BufferedWriter bw = IOUtils.getBufferedWriter("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/nopt/taxiDepartures.csv");
	BufferedWriter bw2 = IOUtils.getBufferedWriter("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/nopt/taxiDestinations.csv");
	for (String c : starts){
		bw.write(c);
		bw.newLine();
		
	}
	bw.flush();
	bw.close();
	for (String c : ends){
		bw2.write(c);
		bw2.newLine();
	}
	bw2.flush();
	bw2.close();
}
}
