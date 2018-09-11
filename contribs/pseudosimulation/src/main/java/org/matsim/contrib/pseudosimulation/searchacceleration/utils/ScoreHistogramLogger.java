/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ScoreHistogramLogger extends AbstractModule implements IterationEndsListener, PersonStuckEventHandler {

	private final Population population;

	private final Config config;

	public ScoreHistogramLogger(final Population population, final Config config) {
		this.population = population;
		this.config = config;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		int noSelectedCnt = 0;
		int notANumberCnt = 0;
		int infiniteCnt = 0;
		final List<Double> scores = new ArrayList<>(this.population.getPersons().size());
		for (Person person : this.population.getPersons().values()) {
			if (person.getSelectedPlan() == null) {
				noSelectedCnt++;
			} else {
				final Double score = person.getSelectedPlan().getScore();
				if (Double.isNaN(score)) {
					notANumberCnt++;
				} else if (Double.isInfinite(score)) {
					infiniteCnt++;
				} else {
					scores.add(score);
				}
			}
		}
		Collections.sort(scores);

		final File file = FileUtils.getFile(this.config.controler().getOutputDirectory(),
				"scores." + event.getIteration() + ".txt");
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			writer.println("has no selected plan: " + noSelectedCnt);
			writer.println("score is NaN: " + notANumberCnt);
			writer.println("score is infinite: " + infiniteCnt);
			writer.println("is stuck: " + this.stuckPersons.size());
			writer.println("total population size: " + this.population.getPersons().size());
			writer.println();
			writer.println("score\tcumulative");
			double cumulative = 0;
			final double increment = 1.0 / scores.size();
			for (Double score : scores) {
				cumulative += increment;
				writer.println(score + "\t" + cumulative);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private final Set<Id<Person>> stuckPersons = new LinkedHashSet<>();

	@Override
	public void reset(int iteration) {
		this.stuckPersons.clear();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (this.population.getPersons().containsKey(event.getPersonId())) {
			this.stuckPersons.add(event.getPersonId());
		}
	}

	@Override
	public void install() {
		addControlerListenerBinding().toInstance(this);
		addEventHandlerBinding().toInstance(this);
	}

}
