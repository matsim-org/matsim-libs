/* *********************************************************************** *
 * project: org.matsim.*
 * IterationStopWatch.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.gbl.Gbl;

/**
 * This class provides a mechanism similar to a stop watch, allowing to measure the duration of operations and
 * remembering time stamps. The class collects all the data and provides a simple analysis of the time stamps
 * and durations for operations for each iteration in the simulation. This analysis can be dumped to console
 * or to a file using the <code>write()</code>-methods.
 *
 * @author mrieser
 */
public class IterationStopWatch {

	/** The current iteration number, or null if not yet initialized. */
	private Integer iteration = null;

	/** The main collection, where all the gathered data is stored. */
	private final Map<Integer, Map<String, Long>> iterations;

	/** A list of identifiers used to enumerate time stamps. */
	private final List<String> identifiers;

	/** A list of identifiers used to enumerate operations. */
	private final List<String> operations;

	/** A cache for easy access to the current object in <code>iterations</code>. */
	private Map<String, Long> currentIterationValues;

	/** The position within <code>identifiers</code>, where the next identifier is expected. */
	private int nextIdentifierPosition = 0;

	/** The position within <code>operations</code>, where the next identifier is expected. */
	private int nextOperationPosition = 0;

	/** A formatter for dates, used when writing out the data. */
	private final DateFormat formatter = new SimpleDateFormat("HH:mm:ss");

	/** Creates a new IterationStopWatch. */
	public IterationStopWatch() {
		this.iterations = new LinkedHashMap<Integer, Map<String,Long>>();
		this.identifiers = new LinkedList<String>();
		this.operations = new LinkedList<String>();
		this.currentIterationValues = null;
	}

	/**
	 * Resets the stop watch, deleting all gathered values.
	 */
	public void reset() {
		this.nextIdentifierPosition = 0;
		this.nextOperationPosition = 0;
		this.iteration = null;
		this.currentIterationValues = null;
		this.iterations.clear();
		this.identifiers.clear();
		this.operations.clear();
	}

	/**
	 * Sets the current iteration, so that the times measured using {@link #beginOperation(String)},
	 * {@link #endOperation(String)} and {@link #timestamp(String)} are assigned to the correct iteration for
	 * the analysis.
	 *
	 * @param iteration
	 */
	public void setCurrentIteration(final int iteration) {
		this.iteration = Integer.valueOf(iteration);
		if (this.iterations.get(this.iteration) == null) {
			this.currentIterationValues = new HashMap<String, Long>();
			this.iterations.put(this.iteration, this.currentIterationValues);
			this.nextIdentifierPosition = 0;
			this.nextOperationPosition = 0;
		}
	}

	/**
	 * Tells the stop watch that an operation begins.
	 *
	 * @param identifier The name of the beginning operation.
	 */
	public void beginOperation(final String identifier) {
		String ident = "BEGIN " + identifier;
		ensureIdentifier(ident);
		this.currentIterationValues.put(ident, Long.valueOf(System.currentTimeMillis()));
	}

	/**
	 * Tells the stop watch that an operation ends. The operation must have been started before with
	 * {@link #beginOperation(String)}.
	 *
	 * @param identifier The name of the ending operation.
	 */
	public void endOperation(final String identifier) {
		String ident = "END " + identifier;
		ensureIdentifier(ident);
		ensureOperation(identifier);
		this.currentIterationValues.put(ident, Long.valueOf(System.currentTimeMillis()));
	}

	/**
	 * Tells the stop watch that a special event happened, for which the time should be remembered.
	 *
	 * @param identifier The name of the event.
	 */
	public void timestamp(final String identifier) {
		ensureIdentifier(identifier);
		this.currentIterationValues.put(identifier, Long.valueOf(System.currentTimeMillis()));
	}

	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream);
		stream.close();
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	public void write(final PrintStream stream) {
		// print header
		stream.print("Iteration");
		for (String identifier : this.identifiers) {
			stream.print('\t');
			stream.print(identifier);
		}
		stream.print('\t');
		for (String identifier : this.operations) {
			stream.print('\t');
			stream.print(identifier);
		}
		stream.println();

		// print data
		for (Map.Entry<Integer, Map<String, Long>> entry : this.iterations.entrySet()) {
			Integer iteration = entry.getKey();
			Map<String, Long> data = entry.getValue();
			// iteration
			stream.print(iteration.toString());
			// identifiers
			for (String identifier : this.identifiers) {
				Long time = data.get(identifier);
				stream.print('\t');
				stream.print(formatMilliTime(time));
			}
			// blank separator
			stream.print('\t');
			// durations of operations
			for (String identifier: this.operations) {
				Long startTime = data.get("BEGIN " + identifier);
				Long endTime = data.get("END " + identifier);
				stream.print('\t');
				if (startTime != null && endTime != null) {
					double diff = (endTime.longValue() - startTime.longValue()) / 1000.0;
					stream.print(Gbl.writeTime(diff));
				}
			}

			// finish
			stream.println();
		}
	}

	/**
	 * Make sure the given identifier exists in our collection. If it is missing, insert it at the correct
	 * place. "Correct" means that it tries to insert this identifier right after the last-requested identifier.
	 * This should help to write the gathered data out in a natural way.
	 *
	 * @param identifier
	 */
	private void ensureIdentifier(final String identifier) {
		int pos = this.identifiers.indexOf(identifier);
		if (pos == -1) {
			this.identifiers.add(this.nextIdentifierPosition, identifier);
			this.nextIdentifierPosition++;
		} else {
			this.nextIdentifierPosition = pos + 1;
		}
	}

	/**
	 * Does the same as {@link #ensureIdentifier(String)}, but for operation-identifiers.
	 *
	 * @param identifier
	 */
	private void ensureOperation(final String identifier) {
		int pos = this.operations.indexOf(identifier);
		if (pos == -1) {
			this.operations.add(this.nextOperationPosition, identifier);
			this.nextOperationPosition++;
		} else {
			this.nextOperationPosition = pos + 1;
		}
	}

	/**
	 * Formats the time given in milliseconds (e.g. returned by {@link java.lang.System#currentTimeMillis()}
	 * nicely for output.
	 *
	 * @param millis A time value in milliseconds
	 * @return A String containing the formatted time, or an empty String if <code>millis</code> is null
	 */
	private String formatMilliTime(final Long millis) {
		if (millis == null) {
			return "";
		}
		return this.formatter.format(new Date(millis.longValue()));
	}

}
