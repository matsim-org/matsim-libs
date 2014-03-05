/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.run;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class KNPopCompare {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Population pop1 ;
		ActivityFacilitiesFactory ff ;
		{
			config.plans().setInputFile(args[0]);
//			if ( args.length>2 && args[2]!=null && args[2]!="" ) {
//				config.network().setInputFile(args[2]);
//			}
			if ( args.length>2 && args[2]!=null && args[2]!="" ) {
				Logger.getLogger(KNPopCompare.class).info("setting person attributes file to: " + args[3] ); 
				config.plans().setInputPersonAttributeFile(args[2]);
			} else {
				throw new RuntimeException("no person attributes") ;
			}

			Scenario scenario1 = ScenarioUtils.loadScenario( config ) ;
			pop1 = scenario1.getPopulation() ;
//			network = scenario1.getNetwork() ;
			ff = ((ScenarioImpl)scenario1).getActivityFacilities().getFactory() ;
		}
		
		Population pop2 ;
		{
			Config config2 = ConfigUtils.createConfig();
			config2.plans().setInputFile(args[1]); 
			if ( args.length>3 && args[3]!=null && args[3]!="" ) {
				Logger.getLogger(KNPopCompare.class).info("setting person attributes file to: " + args[3] ); 
				config2.plans().setInputPersonAttributeFile(args[3]);
			} else {
				throw new RuntimeException("no person attributes") ;
			}
			Scenario scenario1 = ScenarioUtils.loadScenario( config2 ) ;
			pop2 = scenario1.getPopulation() ;
		}

		List<SpatialGrid> spatialGrids = new ArrayList<SpatialGrid>() ;
		ActivityFacilities homesWithScoreDifferences = FacilitiesUtils.createActivityFacilities("scoreDifferencesAtHome") ;
		ActivityFacilities homesWithMoneyDifferences = FacilitiesUtils.createActivityFacilities("moneyDifferencesAtHome") ;
		ActivityFacilities homesWithTtimeDifferences = FacilitiesUtils.createActivityFacilities("ttimeDifferencesAtHome") ;

		// yy could try to first sort the person files and then align them
		for ( Person person1 : pop1.getPersons().values() ) {
			String subPop = (String) pop1.getPersonAttributes().getAttribute(person1.getId().toString(), config.plans().getSubpopulationAttributeName() ) ;
			if ( subPop.equals("car") ) {
				Person person2 = pop2.getPersons().get( person1.getId() ) ;
				if ( person2==null ) {
					throw new RuntimeException( "did not find person in other population") ;
				}

				Activity firstAct = (Activity) person1.getSelectedPlan().getPlanElements().get(0) ;
				Coord coord = firstAct.getCoord() ;
				{
					ActivityFacility fac = ff.createActivityFacility(person1.getId(), coord) ;
					fac.getCustomAttributes().put(GridUtils.WEIGHT, person2.getSelectedPlan().getScore() - person1.getSelectedPlan().getScore() ) ;
					homesWithScoreDifferences.addActivityFacility(fac);
				}
				{
					ActivityFacility fac = ff.createActivityFacility(person1.getId(), coord) ;
					Double money1 = (Double) pop1.getPersonAttributes().getAttribute(person1.getId().toString(), KNAnalysisEventsHandler.MONEY ) ;
					if ( money1==null ) {
						money1 = 0. ;
					}
					Double money2 = (Double) pop2.getPersonAttributes().getAttribute(person1.getId().toString(), KNAnalysisEventsHandler.MONEY ) ;
					if ( money2==null ) {
						money2 = 0. ;
					}
					fac.getCustomAttributes().put(GridUtils.WEIGHT, money2 - money1 ) ; 
					homesWithMoneyDifferences.addActivityFacility(fac);
				}
				{
					ActivityFacility fac = ff.createActivityFacility(person1.getId(), coord) ;
					Double item1 = (Double) pop1.getPersonAttributes().getAttribute(person1.getId().toString(), KNAnalysisEventsHandler.TRAV_TIME ) ;
					if ( item1==null ) {
						item1 = 0. ;
					}
					Double item2 = (Double) pop2.getPersonAttributes().getAttribute(person1.getId().toString(), KNAnalysisEventsHandler.TRAV_TIME ) ;
					if ( item2==null ) {
						item2 = 0. ;
					}
					fac.getCustomAttributes().put(GridUtils.WEIGHT, item2 - item1 ) ;
					homesWithTtimeDifferences.addActivityFacility(fac);
				}
			}
		}

		final BoundingBox bbox = BoundingBox.createBoundingBox( 300000., -2.95e6, 500000., -2.7e6 );
		{
			SpatialGrid spatialGrid = new SpatialGrid( bbox, 2000., 0. ) ; 
			SpatialGrid spatialGridCnt = new SpatialGrid( bbox, 2000., 0. ) ; 
			GridUtils.aggregateFacilitiesIntoSpatialGrid(homesWithScoreDifferences, spatialGrid, spatialGridCnt);
			spatialGrids.add( spatialGrid ) ;
			spatialGrids.add( spatialGridCnt ) ;
		}

		{
			SpatialGrid spatialGrid = new SpatialGrid( bbox, 2000., 0. ) ; 
			SpatialGrid spatialGridCnt = new SpatialGrid( bbox, 2000., 0. ) ; 
			GridUtils.aggregateFacilitiesIntoSpatialGrid(homesWithMoneyDifferences, spatialGrid, spatialGridCnt);
			spatialGrids.add( spatialGrid ) ;
			spatialGrids.add( spatialGridCnt ) ;
		}

		{
			SpatialGrid spatialGrid = new SpatialGrid( bbox, 4000., 0. ) ; 
			SpatialGrid spatialGridCnt = new SpatialGrid( bbox, 4000., 0. ) ; 
			GridUtils.aggregateFacilitiesIntoSpatialGrid(homesWithTtimeDifferences, spatialGrid, spatialGridCnt);
			spatialGrids.add( spatialGrid ) ;
			spatialGrids.add( spatialGridCnt ) ;
		}

		GridUtils.writeSpatialGrids(spatialGrids, "popcompare_grid.csv");


	}


}
