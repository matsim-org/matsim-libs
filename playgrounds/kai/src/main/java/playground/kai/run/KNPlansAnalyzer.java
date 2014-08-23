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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.southafrica.gauteng.GautengTollStatistics;
import playground.southafrica.gauteng.GautengTollStatistics.SimplifiedType;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactorOLD;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;
import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;

/**
 * @author nagel
 *
 */
@Deprecated // uses deprecated SanralTollVehicleType
public class KNPlansAnalyzer {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;

		config.plans().setInputFile(args[0]); 

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		TollFactorI tollFactor = new SanralTollFactorOLD() ;

		Map<SimplifiedType,Double> scoreSum = new HashMap<SimplifiedType,Double>() ;
		Map<SimplifiedType,Double> cnt = new HashMap<SimplifiedType,Double>() ;
		for ( SimplifiedType stype : SimplifiedType.values() ) {
			scoreSum.put( stype, 0. ) ;
			cnt.put( stype, 0. ) ;
		}
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
//			SanralTollVehicleType type = tollFactor.typeOf(person.getId()) ;
			SanralTollVehicleType type = null ; 
			Logger.getLogger(KNPlansAnalyzer.class).fatal("does not work; check code") ;
			System.exit(-1) ;

			SimplifiedType stype = GautengTollStatistics.simplifiedTypeOf( type ) ;
			scoreSum.put( stype, scoreSum.get(stype) + person.getSelectedPlan().getScore() ) ;
			cnt.put( stype,  cnt.get(stype) + 1. ) ;
		}

		for ( SimplifiedType stype : SimplifiedType.values() ) {
			if ( cnt.get(stype) > 0. ) {
				System.out.println( stype.toString() + ": " + scoreSum.get(stype) / cnt.get(stype) ) ;
			}

		}
	}
}