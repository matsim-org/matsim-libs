/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.locationchoice.bestresponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.BestReplyLocationChoice;
import org.matsim.contrib.locationchoice.BestReplyLocationChoice.AttrType;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ComputeKValsAndMaxEpsilon;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ComputeMaxEpsilons;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author nagel
 *
 */
public class LocationChoiceBestResponseContext {
	
	private final Scenario scenario;
	private List<ObjectAttributes> attrs = new ArrayList<ObjectAttributes>() ; // would better be a Map
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private HashSet<String> flexibleTypes;
	private CharyparNagelScoringParameters params;
	private static final Logger log = Logger.getLogger(LocationChoiceBestResponseContext.class);

	public LocationChoiceBestResponseContext(Scenario scenario) {
		this.scenario = scenario;
		
		this.params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore()) ;
		
		ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler(this.scenario.getConfig().locationchoice());
		this.scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();
		this.actTypeConverter = defineFlexibleActivities.getConverter() ;
		this.flexibleTypes = defineFlexibleActivities.getFlexibleTypes() ;
		
		this.readOrCreateObjectAttributes(Long.parseLong(this.scenario.getConfig().locationchoice().getRandomSeed())) ;
	}
	
	private void readOrCreateObjectAttributes(long seed) {
		for ( int ii = 0 ; ii <= 2 ; ii++ ) {
			attrs.add( new ObjectAttributes() );
		}
		
		String pkValuesFileName = this.scenario.getConfig().locationchoice().getpkValuesFile();
		String fkValuesFileName = this.scenario.getConfig().locationchoice().getfkValuesFile();
		String maxEpsValuesFileName = this.scenario.getConfig().locationchoice().getMaxEpsFile();
		if (!pkValuesFileName.equals("null") && !fkValuesFileName.equals("null") && !maxEpsValuesFileName.equals("null")) {			
			ObjectAttributesXmlReader persKValuesReader = new ObjectAttributesXmlReader(attrs.get(AttrType.persKVals.ordinal()));
			ObjectAttributesXmlReader facKValuesReader = new ObjectAttributesXmlReader(attrs.get(AttrType.facKVals.ordinal()));
			ObjectAttributesXmlReader maxEpsReader = new ObjectAttributesXmlReader(attrs.get(AttrType.maxEpsUnsc.ordinal()));
			try {
				persKValuesReader.parse(pkValuesFileName);
				facKValuesReader.parse(fkValuesFileName);
				maxEpsReader.parse(maxEpsValuesFileName);
				log.info("reading kvals and maxEpsilons from files:\n"+ pkValuesFileName + "\n" + fkValuesFileName + "\n" + maxEpsValuesFileName);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				log.error("unsuccessful reading kvals and maxEpsilons from files!\nThe values are now computed" +
				" and following files are not considered!:\n" + pkValuesFileName + "\n" + fkValuesFileName + "\n" + maxEpsValuesFileName);
				attrs = this.computeAttributes(seed);
				return ; // ?? 
			}
		}
		else {
			log.info("Generating kVals and computing maxEpsilons");
			attrs = this.computeAttributes(seed);
			return ;
		}
	}
	
	private List<ObjectAttributes> computeAttributes(long seed) {
		ComputeKValsAndMaxEpsilon computer = new ComputeKValsAndMaxEpsilon(
				seed, this.scenario, this.scaleEpsilon, 
				this.actTypeConverter, this.flexibleTypes);

		computer.run();
		
		// the reason for the following somewhat strange construct is that I want to _return_ the result, rather than
		// having it as a side effect. kai, feb'13
		// (this would now be better as a map)
		List<ObjectAttributes> attributes = new ArrayList<ObjectAttributes>(3) ;
		for ( AttrType type : AttrType.values() ) {
			switch ( type ) {
			case facKVals:
				attributes.add( computer.getFacilitiesKValues() ) ;
				break;
			case maxEpsUnsc:
				attributes.add( computer.getPersonsMaxEpsUnscaled() ) ;
				break;
			case persKVals:
				attributes.add( computer.getPersonsKValues() ) ;
				break;
			}
		}
		
		for ( AttrType type : AttrType.values() ) {
			System.err.println( type.toString() + "\n" + attributes.get(type.ordinal()) ) ;
		}
		
		return attributes ;
		
	}
	
	public Scenario getScenario() {
		return scenario;
	}

	public ObjectAttributes getPersonsKValues() {
		return attrs.get(BestReplyLocationChoice.AttrType.persKVals.ordinal()) ;
	}

	public ObjectAttributes getFacilitiesKValues() {
		return attrs.get(BestReplyLocationChoice.AttrType.facKVals.ordinal()) ;
	}

	public ScaleEpsilon getScaleEpsilon() {
		return scaleEpsilon;
	}

	public ActTypeConverter getConverter() {
		return actTypeConverter;
	}

	public HashSet<String> getFlexibleTypes() {
		return flexibleTypes;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}

	public ObjectAttributes getPersonsMaxEpsUnscaled() {
		return attrs.get(BestReplyLocationChoice.AttrType.maxEpsUnsc.ordinal()) ;
	}
	
	public List<ObjectAttributes> getAttributes() {
		return attrs ;
	}


}
