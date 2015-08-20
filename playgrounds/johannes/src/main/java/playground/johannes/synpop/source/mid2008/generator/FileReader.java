/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.mid2008.generator;

import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.mid.*;
import playground.johannes.synpop.data.*;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class FileReader {

    private final static Logger logger = Logger.getLogger(FileReader.class);

    private final static String DOT = ".";

    private final List<PersonAttributeHandler> personAttHandlers = new ArrayList<>();

    private final List<LegAttributeHandler> legAttHandlers = new ArrayList<>();

    private final List<LegAttributeHandler> journeyAttHandlers = new ArrayList<>();

    private final List<EpisodeAttributeHandler> episodeAttHandlers = new ArrayList<>();

    private final Factory factory;

    private Map<String, Person> persons;

    public FileReader(Factory factory) {
        this.factory = factory;
    }

    public void addPersonAttributeHandler(PersonAttributeHandler handler) {
        personAttHandlers.add(handler);
    }

    public void addLegAttributeHandler(LegAttributeHandler handler) {
        legAttHandlers.add(handler);
    }

    public void addJourneyAttributeHandler(LegAttributeHandler handler) {
        journeyAttHandlers.add(handler);
    }

    public void addEpisodeAttributeHandler(EpisodeAttributeHandler handler) {
        episodeAttHandlers.add(handler);
    }

    public Set<? extends Person> read(String personFile, String legFile, String journeyFile) throws
            IOException {

        persons = new LinkedHashMap<>(65000);
        /*
		 * read and create persons
		 */
        logger.info("Reading persons...");
        new PersonRowHandler().read(personFile);
		/*
		 * read and create legs
		 */
        logger.info("Reading trips...");
        new LegRowHandler().read(legFile);
		/*
		 * read and create journeys
		 */
        logger.info("Reading journeys...");
        new JourneyRowHandler().read(journeyFile);

        return new LinkedHashSet<>(persons.values());
    }

    private String personIdBuilder(Map<String, String> attributes) {
        StringBuilder builder = new StringBuilder(20);
        builder.append(attributes.get(VariableNames.HOUSEHOLD_ID));
        builder.append(DOT);
        builder.append(attributes.get(VariableNames.PERSON_ID));

        return builder.toString();
    }

    private class LegRowHandler extends RowHandler {

        @Override
        protected void handleRow(Map<String, String> attributes) {
            String id = personIdBuilder(attributes);
            Person person = persons.get(id);

            Segment leg = factory.newSegment();
            for (LegAttributeHandler handler : legAttHandlers)
                handler.handle(leg, attributes);

            person.getEpisodes().get(0).addLeg(leg);
        }

    }

    private class PersonRowHandler extends RowHandler {

        @Override
        protected void handleRow(Map<String, String> attributes) {
            Person person = factory.newPerson(personIdBuilder(attributes));

            for (PersonAttributeHandler handler : personAttHandlers) {
                handler.handle(person, attributes);
            }
			/*
		    * add an empty plan to each person
		    */
            Episode episode = factory.newEpisode();
            episode.setAttribute(CommonKeys.DATA_SOURCE, MIDKeys.MID_TRIPS);
            person.addEpisode(episode);

            persons.put(person.getId(), person);
        }

    }

    private class JourneyRowHandler extends RowHandler {

        @Override
        protected void handleRow(Map<String, String> attributes) {
            String id = personIdBuilder(attributes);
            Person person = persons.get(id);

            Episode episode = factory.newEpisode();
            episode.setAttribute(CommonKeys.DATA_SOURCE, MIDKeys.MID_JOUNREYS);
            for (EpisodeAttributeHandler handler : episodeAttHandlers) {
                handler.handle(episode, attributes);
            }

            person.addEpisode(episode);

            Segment leg = factory.newSegment();
            episode.addLeg(leg);
            leg.setAttribute(CommonKeys.LEG_ORIGIN, ActivityType.HOME);
            for (LegAttributeHandler handler : journeyAttHandlers) {
                handler.handle(leg, attributes);
            }
        }

    }
}
