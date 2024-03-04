package org.matsim.contrib.drt.prebooking.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.core.utils.io.IOUtils;

public class PrebookingAnalysisWriter {
	private final String outputPath;

	public PrebookingAnalysisWriter(String outputPath) {
		this.outputPath = outputPath;
	}

	public void write(List<PrebookingAnalysisHandler.RequestRecord> records) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);

			writer.write(String.join(",", new String[] { //
					"request_id", //
					"person_id", //
					"submission_time", //
					"scheduled_time", //
					"rejected_time", //
					"departure_time", //
					"rejected_reason" //
			}) + "\n");

			for (var record : records) {
				writer.write(String.join(",", new String[] { //
						record.requestId().toString(), //
						record.personIds().stream().map(Object::toString).collect(Collectors.joining("-")), //
						record.submissionTime() == null ? "" : String.valueOf(record.submissionTime()), //
						record.scheduledTime() == null ? "" : String.valueOf(record.scheduledTime()), //
						record.rejectedTime() == null ? "" : String.valueOf(record.rejectedTime()), //
						record.departureTime() == null ? "" : String.valueOf(record.departureTime()), //
						record.rejectedReason() //
				}) + "\n");
			}

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
