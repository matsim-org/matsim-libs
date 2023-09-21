package org.matsim.core.replanning.inheritance;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class PlanInheritanceRecordWriter {
	
	public final String AGENT_ID = "agentId";
	public final String PLAN_ID = "planId";
	public final String ANCESTOR_ID = "ancestorId";
	public final String MUTATED_BY = "mutatedBy";
	public final String ITERATION_CREATED = "iterationCreated";
	public final String ITERATION_REMOVED = "iterationRemoved";
	public final String ITERATIONS_SELECTED = "iterationsSelected";
	
	private final Character DELIMITER = '\t';
	private final BufferedWriter writer;
	
	public PlanInheritanceRecordWriter(String filename) {
		this.writer = IOUtils.getBufferedWriter(filename);
		
		StringBuffer header = new StringBuffer();
		header.append(AGENT_ID); header.append(DELIMITER);
		header.append(PLAN_ID); header.append(DELIMITER);
		header.append(ANCESTOR_ID); header.append(DELIMITER);
		header.append(MUTATED_BY); header.append(DELIMITER);
		header.append(ITERATION_CREATED); header.append(DELIMITER);
		header.append(ITERATION_REMOVED); header.append(DELIMITER);
		header.append(ITERATIONS_SELECTED);
		
		try {
			this.writer.write(header.toString());
			this.writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize the plan inheritance writer!", e);
		}
	}

	public void write(PlanInheritanceRecord planInheritanceRecord) {
		StringBuffer line = new StringBuffer();
		line.append(planInheritanceRecord.agentId); line.append(DELIMITER);
		line.append(planInheritanceRecord.planId); line.append(DELIMITER);
		line.append(planInheritanceRecord.ancestorId); line.append(DELIMITER);
		line.append(planInheritanceRecord.mutatedBy); line.append(DELIMITER);
		line.append(planInheritanceRecord.iterationCreated); line.append(DELIMITER);
		line.append(planInheritanceRecord.iterationRemoved); line.append(DELIMITER);
		line.append(planInheritanceRecord.iterationsSelected);
		
		try {
			this.writer.write(line.toString());
			this.writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize the plan inheritance writer!", e);
		}
	}

	public void flush() {
		try {
			this.writer.flush();
		} catch (IOException e) {
			throw new RuntimeException("Failed to flush plan inheritance writer!", e);
		}
	}

	public void close() {
		try {
			this.writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed to close plan inheritance writer!", e);
		}
	}
}
