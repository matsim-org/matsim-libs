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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter.ChoiceSetRecordFiller;

import java.io.IOException;

/**
 * @author thibautd
 */
public class Converter<T,C extends ChoiceSituation<T>> {
	private final ChoiceSetSampler<T,C> choiceSetSampler;
	private final ChoiceSetRecordFiller<T> recordFiller;
	private final ChoicesIdentifier<C> choicesIdentifier;

	private Converter(
			final ChoiceSetSampler<T,C> choiceSetSampler,
			final ChoiceSetRecordFiller<T> recordFiller,
			final ChoicesIdentifier<C> choicesIdentifier) {
		this.choiceSetSampler = choiceSetSampler;
		this.recordFiller = recordFiller;
		this.choicesIdentifier = choicesIdentifier;
	}

	public static <T,C extends ChoiceSituation<T>> ConverterBuilder<T,C> builder() {
		return new ConverterBuilder<>();
	}

	public void convert( final Population chains , final String dataset ) {
		try ( final ChoiceDataSetWriter<T> writer = new ChoiceDataSetWriter<>( recordFiller , dataset ) ) {
			for (Person p : chains.getPersons().values() ) {
				for ( C choice : choicesIdentifier.identifyChoices(p.getSelectedPlan()) ) {
					writer.write( choiceSetSampler.sampleChoiceSet( p , choice ) );
				}
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public static class ConverterBuilder<T,C extends ChoiceSituation<T>> {
		private ChoiceSetSampler<T,C> choiceSetSampler;
		private ChoiceSetRecordFiller<T> recordFiller;
		private ChoicesIdentifier<C> choicesIdentifier;

		public ConverterBuilder<T,C> withChoiceSetSampler(final ChoiceSetSampler<T,C> choiceSetSampler) {
			this.choiceSetSampler = choiceSetSampler;
			return this;
		}

		public ConverterBuilder<T,C> withRecordFiller(final ChoiceSetRecordFiller<T> recordFiller) {
			this.recordFiller = recordFiller;
			return this;
		}

		public ConverterBuilder<T,C> withChoicesIdentifier(final ChoicesIdentifier<C> choicesIdentifier) {
			this.choicesIdentifier = choicesIdentifier;
			return this;
		}

		public Converter<T,C> create() {
			return new Converter(choiceSetSampler, recordFiller, choicesIdentifier);
		}
	}
}
