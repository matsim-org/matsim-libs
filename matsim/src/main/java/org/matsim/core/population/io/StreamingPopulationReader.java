/* *********************************************************************** *
 * project: org.matsim.*
 * Plans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class StreamingPopulationReader implements MatsimReader {
	private static final Logger log = LogManager.getLogger(StreamingPopulationReader.class);
	
	private PopulationReader reader ;
	private final StreamingPopulation pop ;
	private int cnt;
	private final Map<Class<?>, AttributeConverter<?>> attributeConverters = new HashMap<>();

	// algorithms over plans
	private final ArrayList<PersonAlgorithm> personAlgos = new ArrayList<>();

	public StreamingPopulationReader(Scenario scenario ) {
	    // should we convert to global by default or not? Optimal seems to depend on usecase...
		this( null, null, scenario ) ;
	}

	public StreamingPopulationReader(String inputCRS, String targetCRS, Scenario scenario ) {
		if ( scenario instanceof MutableScenario ) {
			pop = new StreamingPopulation( scenario.getConfig() ) ;
			((MutableScenario) scenario).setPopulation(pop);
			reader = new PopulationReader( inputCRS, targetCRS, scenario, true) ;
		} else {
			throw new RuntimeException("scenario given into this class needs to be an instance of MutableScenario.") ;
		}
	}

	public void putAttributeConverter(final Class<?> clazz, AttributeConverter<?> converter) {
		this.attributeConverters.put(clazz, converter);
	}

	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		this.attributeConverters.putAll(converters);
	}

	Population getStreamingPopulation() {
		return pop ;
	}
	@Override public void readFile(String filename) {
		reader.putAttributeConverters(this.attributeConverters);
		reader.readFile(filename);
	}

	@Override
	public void readURL( URL url ) {
		reader.putAttributeConverters(this.attributeConverters);
		reader.parse( url ) ;
	}

	public void parse(InputStream is) {
		reader.parse(is);
	}

	public void parse(URL url) {
		reader.parse( url );
	}

	//////////////////////////////////////////////////////////////////////
	// algorithms
	//////////////////////////////////////////////////////////////////////

	public final void clearAlgorithms() {
		this.personAlgos.clear();
	}
	public final void addAlgorithm(final PersonAlgorithm algo) {
		this.personAlgos.add(algo);
	}


	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////


	@SuppressWarnings("static-method")
	@Deprecated // dec'16
	public final boolean isStreaming() {
		return true ;
	}

	@SuppressWarnings("static-method")
	@Deprecated // dec'16
	final void setIsStreaming(final boolean isStreaming) {
		if ( isStreaming==false ) {
			log.error( "streaming == false currently not supported with this approach; please program it yourself; something like" ) ;
			log.error( "for ( Person person : pop.getPersons().values() ) {" ) ;
			log.error( "   personAlgo.run( person ); " ) ;
			log.error( "}" );
			throw new RuntimeException("streaming == false currently not supported with this approach; see log statements" ) ;
		}
	}
	@Override public final String toString() {
		return "[nof_plansalgos=" + this.personAlgos.size() + "]";
	}

	final class StreamingPopulation implements Population {
		private Population delegate ;
		
		StreamingPopulation(Config config) {
			delegate = PopulationUtils.createPopulation(config) ;
		}
		
		@Override
		public final void addPerson(final Person p) {
			
			
			cnt++ ;

//			if (!this.isStreaming) {
//				// streaming is off, just add the person to our list
//				pop.addPerson(p);
//			} else {
				// streaming is on, run algorithm on the person and write it to file.

				/* Add Person to map, for algorithms might reference to the person
				 * with "agent = population.getPersons().get(personId);"
				 * remove it after running the algorithms! */
				delegate.addPerson(p);
				// (yyyyyy do we really need this?  Also, does it make a lot of sense?
				// pop.getPerson( current ) will then work, but pop.getPerson( other ) will not work.  Might be better to just get rid of this
				// completely, no?  kai, jul'16)

				// run algos
				for (PersonAlgorithm algo : personAlgos) {
					algo.run(p);
				}

				// remove again as we are streaming
				delegate.removePerson(p.getId());
//			}

		}
		@Override public PopulationFactory getFactory() {
			return delegate.getFactory();
		}
		@Override public String getName() {
			return delegate.getName();
		}
		@Override public void setName(String name) {
			delegate.setName(name);
		}
		@Override public Map<Id<Person>, ? extends Person> getPersons() {
			return delegate.getPersons();
		}
		@Override public Person removePerson(Id<Person> personId) {
			return delegate.removePerson(personId);
		}

		@Override
		public Attributes getAttributes() {
			return delegate.getAttributes();
		}
	}

	@Deprecated // do not use from outside; will be removed as public functionality eventually yy
	public void printPlansCount() {
		log.info("processed " + cnt + " persons.");
	}

}
