package org.matsim.counts;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultiModeCountsWriter extends MatsimXmlWriter {

	private final MultiModeCounts counts;

	public MultiModeCountsWriter(MultiModeCounts counts){
		this.counts = counts;
	}

	public void write(String filename){
		this.useCompression = filename.endsWith(".gz");

		this.writer = IOUtils.getBufferedWriter(filename);

		writeXmlHead();

		writeStartTag(MultiModeCounts.ELEMENT_NAME, attributesAsTupleList(counts), false, true);

		writeCounts();
		writeEndTag(MultiModeCounts.ELEMENT_NAME);
	}

	private void writeCounts() {

		for (Object o : counts.getCounts().values()) {

			if(o instanceof MultiModeCount count){
				writeStartTag(MultiModeCount.ELEMENT_NAME, attributesAsTupleList(count), false, true);
				//write volumes for each mode

				writeEndTag(MultiModeCount.ELEMENT_NAME);
			}
		}

	}

	private List<Tuple<String, String>> attributesAsTupleList(Attributable attributable) {
		List<Tuple<String, String>> result = new ArrayList<>();

		for (Map.Entry<String, Object> entry : attributable.getAttributes().getAsMap().entrySet()) {
			Tuple<String, String> tuple = new Tuple<>(entry.getKey(), entry.getValue().toString());
			result.add(tuple);
		}

		return result;
	}

	public void write(Path filename){
		write(filename.toString());
	}
}
