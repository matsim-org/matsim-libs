package org.matsim.core.replanning.inheritance;

import java.io.BufferedReader;

/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPopulationReaderMatsimV6.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.io.IOUtils;

/**
 * Writes {@linkplain PlanInheritanceRecord} to file in a fixed column sequence.
 *
 * @author alex94263
 */
public class PlanInheritanceRecordReader {


	private final String DELIMITER = "\t";
	private final BufferedReader reader;

	public PlanInheritanceRecordReader(String filename) {
		this.reader = IOUtils.getBufferedReader(filename);



	}
	public Map<String, Integer> buildIdx(String[] header) {
		Map<String, Integer> lookup = new HashMap<String,Integer>();
		for (int i=0; i<header.length; i++) {
			lookup.put(header[i],i);
		}
		return lookup;
	}

	public List<PlanInheritanceRecord> read() {
		List<PlanInheritanceRecord> records = new ArrayList<PlanInheritanceRecord>();
		try {
			Map<String,Integer> lookUp = buildIdx(reader.readLine().split(DELIMITER));
			String lineString = reader.readLine();
			while(lineString !=null) {
				String[] line = lineString.split(DELIMITER);
				PlanInheritanceRecord planInheritanceRecord = new PlanInheritanceRecord();
				planInheritanceRecord.setAgentId(Id.createPersonId(line[lookUp.get(PlanInheritanceRecordWriter.AGENT_ID)]));
				planInheritanceRecord.setPlanId(Id.create(line[lookUp.get(PlanInheritanceRecordWriter.PLAN_ID)], Plan.class));
				planInheritanceRecord.setAncestorId(Id.create(line[lookUp.get(PlanInheritanceRecordWriter.ANCESTOR_ID)], Plan.class));
				planInheritanceRecord.setMutatedBy(line[lookUp.get(PlanInheritanceRecordWriter.MUTATED_BY)]);
				planInheritanceRecord.setIterationCreated(Integer.parseInt(line[lookUp.get(PlanInheritanceRecordWriter.ITERATION_CREATED)]));
				planInheritanceRecord.setIterationRemoved(Integer.parseInt(line[lookUp.get(PlanInheritanceRecordWriter.ITERATION_REMOVED)]));
				String iterationsSelected = line[lookUp.get(PlanInheritanceRecordWriter.ITERATIONS_SELECTED)];
				planInheritanceRecord.setIterationsSelected(Arrays.asList(iterationsSelected.substring(1, iterationsSelected.length()-1).split(", ")).stream()
			            .map(Integer::parseInt)
			            .collect(Collectors.toList()));
				records.add(planInheritanceRecord);
				lineString = reader.readLine();
			}
			return records;

		} catch (IOException e) {
			throw new RuntimeException("Could not read the plan inheritance records!", e);
		}



	}


}
