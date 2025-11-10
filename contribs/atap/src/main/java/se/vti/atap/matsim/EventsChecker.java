/**
 * org.matsim.contrib.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.matsim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class EventsChecker implements BasicEventHandler, AfterMobsimListener {

	private final boolean reactToReset;

	private final Map<Id<Person>, List<HasPersonId>> personId2event = new LinkedHashMap<>();

	private Set<Id<Person>> observedPersonIds = new LinkedHashSet<>();

	@Override
	public void reset(int iteration) {
		if (this.reactToReset) {
			this.personId2event.clear();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof HasPersonId) {
			final HasPersonId personEvent = (HasPersonId) event;
			if (this.observedPersonIds.contains(personEvent.getPersonId())) {
				this.personId2event.computeIfAbsent(personEvent.getPersonId(), e -> new ArrayList<>()).add(personEvent);
			}
		}
	}

	public static void generateObservedPersonIds(Population population, int count, String fileName) {
		List<Id<Person>> allIdList = new ArrayList<>(population.getPersons().keySet());
		Collections.shuffle(allIdList);
		try {
			PrintWriter writer = new PrintWriter(fileName);
			for (int i = 0; i < count; i++) {
				writer.println(allIdList.get(i));
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public EventsChecker(String fileName, boolean reactToReset) {
		this.reactToReset = reactToReset;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				this.observedPersonIds.add(Id.createPersonId(currentLine));
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeReport(String fileName) {
		try {
			PrintWriter writer = new PrintWriter(fileName);
			for (Map.Entry<Id<Person>, List<HasPersonId>> entry : this.personId2event.entrySet()) {
				writer.println(entry.getKey());
				if (entry.getValue() != null) {
					for (HasPersonId event : entry.getValue()) {
						writer.println(event);
					}
				}
				writer.println();
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		this.writeReport("simulatedEventsReport." + event.getIteration() + ".txt");
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.plans()
				.setInputFile("C:\\Users\\GunnarF\\OneDrive - VTI\\My Data\\ihop4\\1PctAllModes_enriched_FIXED.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		EventsChecker.generateObservedPersonIds(population, 5, "observedPersons.txt");
	}
}
