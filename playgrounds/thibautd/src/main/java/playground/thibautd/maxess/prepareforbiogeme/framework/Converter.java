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
package playground.thibautd.maxess.prepareforbiogeme.framework;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter.ChoiceSetRecordFiller;

import java.io.IOException;
import java.util.List;

/**
 * @author thibautd
 */
public class Converter<T> {
	private final ChoiceSetSampler<T> choiceSetSampler;
	private final ChoiceSetRecordFiller<T> recordFiller;
	private final ChoicesIdentifier<T> choicesIdentifier;

	private Converter(
			final ChoiceSetSampler<T> choiceSetSampler,
			final ChoiceSetRecordFiller<T> recordFiller,
			final ChoicesIdentifier<T> choicesIdentifier) {
		this.choiceSetSampler = choiceSetSampler;
		this.recordFiller = recordFiller;
		this.choicesIdentifier = choicesIdentifier;
	}

	public static <T> ConverterBuilder<T> builder() {
		return new ConverterBuilder<>();
	}

	public void convert( final Population chains , final String dataset ) {
		try ( final ChoiceDataSetWriter<T> writer = new ChoiceDataSetWriter<>( recordFiller , dataset ) ) {
			for (Person p : chains.getPersons().values() ) {
				for ( T choice : choicesIdentifier.indentifyChoices( p.getSelectedPlan() ) ) {
					writer.write( choiceSetSampler.sampleChoiceSet( p , choice ) );
				}
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public interface ChoicesIdentifier<T> {
		List<T> indentifyChoices( final Plan p );
	}

	public static class ConverterBuilder<T> {
		private ChoiceSetSampler<T> choiceSetSampler;
		private ChoiceSetRecordFiller<T> recordFiller;
		private ChoicesIdentifier<T> choicesIdentifier;

		public ConverterBuilder<T> withChoiceSetSampler(final ChoiceSetSampler<T> choiceSetSampler) {
			this.choiceSetSampler = choiceSetSampler;
			return this;
		}

		public ConverterBuilder<T> withRecordFiller(final ChoiceSetRecordFiller<T> recordFiller) {
			this.recordFiller = recordFiller;
			return this;
		}

		public ConverterBuilder<T> withChoicesIdentifier(final ChoicesIdentifier<T> choicesIdentifier) {
			this.choicesIdentifier = choicesIdentifier;
			return this;
		}

		public Converter<T> create() {
			return new Converter(choiceSetSampler, recordFiller, choicesIdentifier);
		}
	}
}
