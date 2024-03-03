/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import com.google.common.base.Joiner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.LinkStatsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * @author mrieser
 */
final class LinkStatsControlerListener implements IterationEndsListener, IterationStartsListener, ShutdownListener {

	@Inject
	private LinkStatsConfigGroup linkStatsConfigGroup;
	@Inject
	private ControllerConfigGroup controllerConfigGroup;
	@Inject
	private CalcLinkStats linkStats;
	@Inject
	private VolumesAnalyzer volumes;
	@Inject
	private OutputDirectoryHierarchy controlerIO;
	@Inject
	private Map<String, TravelTime> travelTime;

	@Inject
	private Scenario scenario;

	private int iterationsUsed = 0;
	private boolean doReset = false;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();

		if (useVolumesOfIteration(iteration, controllerConfigGroup.getFirstIteration())) {
			this.iterationsUsed++;
			linkStats.addData(volumes, travelTime.get(TransportMode.car));
		}

		if (createLinkStatsInIteration(iteration)) {
			linkStats.writeFile(this.controlerIO.getIterationFilename(iteration, Controler.DefaultFiles.linkstats));
			this.doReset = true;
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.doReset) {
			// resetting at the beginning of an iteration, to allow others to use the data until the very end of the previous iteration
			this.linkStats.reset();
			this.doReset = false;
		}
	}

	/*package*/ boolean useVolumesOfIteration(final int iteration, final int firstIteration) {
		if (this.linkStatsConfigGroup.getWriteLinkStatsInterval() < 1) {
			return false;
		}
		int iterationMod = iteration % this.linkStatsConfigGroup.getWriteLinkStatsInterval();
		int effectiveIteration = iteration - firstIteration;
		int averaging = Math.min(this.linkStatsConfigGroup.getAverageLinkStatsOverIterations(), this.linkStatsConfigGroup.getWriteLinkStatsInterval());
		if (iterationMod == 0) {
			return ((this.linkStatsConfigGroup.getAverageLinkStatsOverIterations() <= 1) ||
					(effectiveIteration >= averaging));
		}
		return (iterationMod > (this.linkStatsConfigGroup.getWriteLinkStatsInterval() - this.linkStatsConfigGroup.getAverageLinkStatsOverIterations())
				&& (effectiveIteration + (this.linkStatsConfigGroup.getWriteLinkStatsInterval() - iterationMod) >= averaging));
	}

	/*package*/ boolean createLinkStatsInIteration(final int iteration) {
		return ((iteration % this.linkStatsConfigGroup.getWriteLinkStatsInterval() == 0) && (this.iterationsUsed >= this.linkStatsConfigGroup.getAverageLinkStatsOverIterations()));
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		String fileName = this.controlerIO.getOutputFilename(Controler.DefaultFiles.linkscsv);
		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setDelimiter(event.getServices().getConfig().global().getDefaultDelimiter().charAt(0))
				.build();

		List<String> attributes = scenario.getNetwork().getLinks().values().parallelStream().flatMap(p -> p.getAttributes().getAsMap().keySet().stream()).distinct().toList();
		Set<String> modes = volumes.getModes();

		List<String> header = new ArrayList<>(List.of("link", "from_node", "to_node", "length", "freespeed", "capacity", "lanes", "modes"));

		for (String mode : modes) {
			header.add("vol_" + mode);
		}

		header.addAll(attributes);
		header.add("geometry");

		Joiner joiner = Joiner.on(",");

		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName), format)) {

			printer.printRecord(header);
			for (Link link : scenario.getNetwork().getLinks().values()) {

				List<Object> row = new ArrayList<>();
				row.add(link.getId());
				row.add(link.getFromNode().getId());
				row.add(link.getToNode().getId());
				row.add(link.getLength());
				row.add(link.getFreespeed());
				row.add(link.getCapacity());
				row.add(link.getNumberOfLanes());
				row.add(joiner.join(link.getAllowedModes()));

				// Sum for each mode
				for (String mode : modes) {
					int[] vol = volumes.getVolumesForLink(link.getId(), mode);
					if (vol == null) {
						row.add(0);
					} else {
						row.add(Arrays.stream(vol).sum());
					}
				}

				for (String attr : attributes) {
					row.add(link.getAttributes().getAttribute(attr));
				}

				row.add(getLinkGeometry(link));

				printer.printRecord(row);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * WKT link geometry.
	 */
	private String getLinkGeometry(Link link) {
		return "LINESTRING( " + link.getFromNode().getCoord().getX() + " " + link.getFromNode().getCoord().getY() +
				", " + link.getToNode().getCoord().getX() + " " + link.getToNode().getCoord().getY() + " )";
	}

}
