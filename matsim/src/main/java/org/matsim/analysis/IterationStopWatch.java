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

import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.core.utils.charts.StackedBarChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class provides a mechanism similar to a stop watch, allowing to measure the duration of operations and
 * remembering time stamps. The class collects all the data and provides a simple analysis of the time stamps
 * and durations for operations for each iteration in the simulation. This analysis can be dumped to console
 * or to a file using the <code>write()</code>-methods.
 *
 * @author mrieser
 */
public final class IterationStopWatch {
	/**
	 * Strings used to identify the operations in the IterationStopWatch.
	 */
	public static final String OPERATION_ITERATION = "iteration";

	/*
	 * Time spent in operations which are not measured in detail.
	 */
	public static final String OPERATION_OTHER = "other";

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

	/** data structures to identify nested operations */
	private Stack<String> currentMeasuredOperations;
	private Map<String, List<String>> currentIterationChildren;
	private final Map<Integer, Map<String, List<String>>> children;

	/** Creates a new IterationStopWatch. */
	public IterationStopWatch() {
		this.iterations = new LinkedHashMap<>();
		this.identifiers = new LinkedList<>();
		this.operations = new LinkedList<>();
		this.currentIterationValues = null;
		this.children = new LinkedHashMap<>();
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
		this.currentMeasuredOperations.clear();
		this.currentIterationChildren.clear();
		this.children.clear();
	}

	/**
	 * Sets the current iteration, so that the times measured using {@link #beginOperation(String)},
	 * {@link #endOperation(String)} and {@link #timestamp(String)} are assigned to the correct iteration for
	 * the analysis.
	 */
	public void beginIteration(final int iteration) {
		this.iteration = iteration;
		if (this.iterations.get(this.iteration) == null) {
			this.currentIterationValues = new HashMap<>();
			this.iterations.put(this.iteration, this.currentIterationValues);
			this.nextIdentifierPosition = 0;
			this.nextOperationPosition = 0;
			this.currentMeasuredOperations = new Stack<>();
			this.currentIterationChildren = new HashMap<>();
			this.children.put(this.iteration, this.currentIterationChildren);
		}
        this.beginOperation(OPERATION_ITERATION);
	}

	/**
	 * Tells the stop watch that an operation begins.
	 *
	 * @param identifier The name of the beginning operation.
	 */
	public void beginOperation(final String identifier) {

		if (identifier.equals(OPERATION_OTHER)) {
			throw new RuntimeException("Identifier " + OPERATION_OTHER + " is reserved! Please use another one. Aborting!");
		}

		String ident = "BEGIN " + identifier;
		ensureIdentifier(ident);
		this.currentIterationValues.put(ident, System.currentTimeMillis());

		this.currentIterationChildren.put(identifier, new ArrayList<>());

		// check whether this operation has a parent operation
		if (!this.currentMeasuredOperations.isEmpty()) {
			String parent = this.currentMeasuredOperations.peek();
			this.currentIterationChildren.get(parent).add(identifier);
		}

		// add ident to stack
		this.currentMeasuredOperations.push(identifier);
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
		this.currentIterationValues.put(ident, System.currentTimeMillis());


		this.currentMeasuredOperations.pop();
	}

    public void endIteration() {
        this.endOperation(OPERATION_ITERATION);
    }

	/**
	 * Tells the stop watch that a special event happened, for which the time should be remembered.
	 *
	 * @param identifier The name of the event.
	 */
	public void timestamp(final String identifier) {
		ensureIdentifier(identifier);
		this.currentIterationValues.put(identifier, System.currentTimeMillis());
	}

	/**
	 * Writes the gathered data into a file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 * @param delimiter The delimiter to be used as field separator.
	 */
	public void writeSeparatedFile(final String filename, final String delimiter) {
		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {

			// print header
			writer.write("iteration");
			for (String identifier : this.identifiers) {
				writer.write(delimiter);
				writer.write(identifier);
			}
			writer.write(delimiter);
			for (String identifier : this.operations) {
				writer.write(delimiter);
				writer.write(identifier);
			}
			writer.write('\n');

			// print data
			for (Map.Entry<Integer, Map<String, Long>> entry : this.iterations.entrySet()) {
				Integer iteration = entry.getKey();
				Map<String, Long> data = entry.getValue();
				// iteration
				writer.write(iteration.toString());
				// identifiers
				for (String identifier : this.identifiers) {
					Long time = data.get(identifier);
					writer.write(delimiter);
					writer.write(formatMilliTime(time));
				}
				// blank separator
				writer.write(delimiter);
				// durations of operations
				for (String identifier: this.operations) {
					Long startTime = data.get("BEGIN " + identifier);
					Long endTime = data.get("END " + identifier);
					writer.write(delimiter);
					if (startTime != null && endTime != null) {
						double diff = (endTime - startTime) / 1000.0;
						writer.write(Time.writeTime(diff));
					}
				}

				// finish
				writer.write("\n");
			}
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes the gathered data as graph into a png file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void writeGraphFile(String filename) {

		int iterations = this.iterations.entrySet().size();
		Map<String, double[]> arrayMap = new HashMap<>();
		for (String identifier : this.operations) arrayMap.put(identifier, new double[iterations]);

		int iter = 0;
		for(Entry<Integer, Map<String, Long>> entry : this.iterations.entrySet()) {
			Map<String, Long> data = entry.getValue();

			// children map of current iteration
			Map<String, List<String>> childrenMap = this.children.get(entry.getKey());

			// durations of operations
			for (String identifier : this.operations) {
				Long startTime = data.get("BEGIN " + identifier);
				Long endTime = data.get("END " + identifier);
				if (startTime != null && endTime != null) {
					double diff = (endTime - startTime);

					/*
					 * If the operation has children, subtract their durations since they are
					 * also included in the plot. Otherwise, their duration would be counted twice.
					 */
					for (String child : childrenMap.get(identifier)) {
						Long childStartTime = data.get("BEGIN " + child);
						Long childEndTime = data.get("END " + child);
						diff -= (childEndTime - childStartTime);
					}

					arrayMap.get(identifier)[iter] = diff / 1000.0;
				} else arrayMap.get(identifier)[iter] = 0.0;
			}
			iter++;
		}

		String title = "Computation time distribution per iteration";
		String xAxisLabel = "iteration";
		String yAxisLabel = "seconds";

		String[] categories = new String[this.iterations.size()];
		int index = 0;
		for (int iteration : this.iterations.keySet()) {
			categories[index] = String.valueOf(iteration);
			index++;
		}

		StackedBarChart chart = new StackedBarChart(title, xAxisLabel, yAxisLabel, categories);
		chart.addMatsimLogo();

		/*
		 * Rotate x-axis labels by 90° which should allow more of them to be plotted before overlapping.
		 * However, a more general solution that also is able to skip labels would be nice.
		 * cdobler nov'13
		 */
		chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

		double[] iterationData = null;
		for (String operation : this.operations) {
			double[] data = arrayMap.get(operation);
			if (operation.equals(OPERATION_ITERATION)) {
				iterationData = data;
			} else {
				chart.addSeries(operation, data);
			}
		}
		if (iterationData != null) {
			double[] otherData = new double[iterations];
			System.arraycopy(iterationData, 0, otherData, 0, iterations);
			chart.addSeries(OPERATION_OTHER, otherData);
		}

		chart.saveAsPng(filename + ".png", 1024, 768);
	}

	/**
	 * Make sure the given identifier exists in our collection. If it is missing, insert it at the correct
	 * place. "Correct" means that it tries to insert this identifier right after the last-requested identifier.
	 * This should help to write the gathered data out in a natural way.
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
	 * Formats the time given in milliseconds (e.g. returned by {@link java.lang.System#currentTimeMillis()})
	 * nicely for output.
	 *
	 * @param millis A time value in milliseconds
	 * @return A String containing the formatted time, or an empty String if <code>millis</code> is null
	 */
	private String formatMilliTime(final Long millis) {
		if (millis == null) {
			return "";
		}
		return this.formatter.format(new Date(millis));
	}

}
