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
package playground.ivt.maxess.prepareforbiogeme.framework;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter.ChoiceSetRecordFiller;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author thibautd
 */
public class Converter<T,C extends ChoiceSituation<T>> {
	private static final Logger log = Logger.getLogger(Converter.class);
	private final ThreadLocal<ChoiceSetSampler<T,C>> choiceSetSampler;
	private final ChoiceSetRecordFiller<T> recordFiller;
	private final ThreadLocal<ChoicesIdentifier<C>> choicesIdentifier;

	private final int nThreads;

	private Converter(
			final int nThreads,
			final Provider<ChoiceSetSampler<T,C>> choiceSetSampler,
			final ChoiceSetRecordFiller<T> recordFiller,
			final Provider<ChoicesIdentifier<C>> choicesIdentifier) {
		this.nThreads = nThreads;
		this.choiceSetSampler = new ThreadLocal<ChoiceSetSampler<T, C>>() {
			@Override
			protected ChoiceSetSampler<T, C> initialValue() {
				return choiceSetSampler.get();
			}
		};
		this.recordFiller = recordFiller;
		this.choicesIdentifier = new ThreadLocal<ChoicesIdentifier<C>>() {
			@Override
			protected ChoicesIdentifier<C> initialValue() {
				return choicesIdentifier.get();
			}
		};
	}

	public static <T,C extends ChoiceSituation<T>> ConverterBuilder<T,C> builder() {
		return new ConverterBuilder<>();
	}

	public void convert( final Population chains , final String dataset ) {
		final ExecutorService executor = Executors.newFixedThreadPool( nThreads );

		log.info( "start conversion" );
		try ( final ChoiceDataSetWriter<T> writer = new ChoiceDataSetWriter<>( recordFiller , dataset ) ) {
			log.info( "start data writing thread" );
			final WritingRunnable runnable = new WritingRunnable( writer );
			final Thread writerThread = new Thread( runnable );

			writerThread.start();

			log.info( "Submit tasks to worker threads" );
			final Counter counter = new Counter( "Submitting task # " );
			for (Person p : chains.getPersons().values() ) {
				counter.incCounter();
				final Person pf = p;
				if ( pf.getPlans().isEmpty() ) {
					log.warn( "Person "+pf+" has no plans! Ignoring it..." );
					continue;
				}

				executor.execute(
						new Runnable() {
							@Override
							public void run() {
								try {
									for (C choice : choicesIdentifier.get().identifyChoices(pf.getSelectedPlan())) {
										final ChoiceSet<T> set = choiceSetSampler.get().sampleChoiceSet(pf, choice);
										runnable.queue.add(set);
									}
								}
								catch (Throwable t) {
									// Bypass an anoying feature of the Executor, that creates new threads if a thread
									// crashes. We want to stop fast if something occurs.
									executor.shutdownNow();
									runnable.doRun = false;
									throw t;
								}
							}
						});
			}
			counter.printCounter();

			log.info( "all tasks submitted. Waiting for execution." );
			executor.shutdown();
			executor.awaitTermination( Long.MAX_VALUE , TimeUnit.DAYS );
			log.info( "all tasks executed." );

			log.info( "Wait for writer thread to finish." );
			runnable.doRun = false;
			writerThread.join();
			log.info( "done with conversion" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	private class  WritingRunnable implements Runnable {
		private final BlockingQueue< ChoiceSet<T> > queue = new LinkedBlockingQueue<>();
		private boolean doRun = true;
		final ChoiceDataSetWriter<T> writer;

		private WritingRunnable(ChoiceDataSetWriter<T> writer) {
			this.writer = writer;
		}

		@Override
		public void run() {
			try {
				log.info( "Writing thread started");
				while ( doRun || !queue.isEmpty() ) {
					final ChoiceSet<T> elem = queue.poll( 1 , TimeUnit.SECONDS );
					if ( elem != null ) writer.write( elem );
				}
				log.info( "Writing thread completed");
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		}
	}

	public static class ConverterBuilder<T,C extends ChoiceSituation<T>> {
		private Provider<ChoiceSetSampler<T,C>> choiceSetSampler;
		private ChoiceSetRecordFiller<T> recordFiller;
		private Provider<ChoicesIdentifier<C>> choicesIdentifier;
		private int nThreads = 1;

		public ConverterBuilder<T,C> withChoiceSetSampler(final Provider<ChoiceSetSampler<T,C>> choiceSetSampler) {
			this.choiceSetSampler = choiceSetSampler;
			return this;
		}

		public ConverterBuilder<T,C> withRecordFiller(final ChoiceSetRecordFiller<T> recordFiller) {
			this.recordFiller = recordFiller;
			return this;
		}

		public ConverterBuilder<T,C> withChoicesIdentifier(final Provider<ChoicesIdentifier<C>> choicesIdentifier) {
			this.choicesIdentifier = choicesIdentifier;
			return this;
		}

		public ConverterBuilder<T,C> withNumberOfThreads(final int nThreads) {
			this.nThreads = nThreads;
			return this;
		}

		public Converter<T,C> create() {
			return new Converter(nThreads, choiceSetSampler, recordFiller, choicesIdentifier);
		}
	}
}
