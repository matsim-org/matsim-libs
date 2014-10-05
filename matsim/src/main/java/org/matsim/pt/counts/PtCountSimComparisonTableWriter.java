package org.matsim.pt.counts;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.counts.CountSimComparison;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PtCountSimComparisonTableWriter implements MatsimWriter {

	private List<CountSimComparison> countSimCompList;

	public PtCountSimComparisonTableWriter(List<CountSimComparison> countSimCompList) {
		this.countSimCompList = countSimCompList;
	}

	@Override
	public void write(String filename) {
		SimpleWriter simpleWriter = null;
		try {
			simpleWriter = new SimpleWriter(filename);
			Id<TransitStopFacility> lastStopId = null;
			// Sorted by stopId
			for (CountSimComparison count : countSimCompList) {
				Id<TransitStopFacility> stopId = Id.create(count.getId(), TransitStopFacility.class);
				if (!stopId.equals(lastStopId)) {
					simpleWriter.write("StopId :\t");
					simpleWriter.write(stopId.toString());
					simpleWriter.write("\nhour\tsimVal\tscaledSimVal\tcountVal\n");
					lastStopId = stopId;
				}
				simpleWriter.write(count.getHour());
				simpleWriter.write('\t');

				double countValue = count.getCountValue();
				double simValue = count.getSimulationValue();

				simpleWriter.write(simValue);
				simpleWriter.write('\t');

				simpleWriter.write(simValue);
				simpleWriter.write('\t');
				simpleWriter.write(countValue);
				simpleWriter.write('\n');

			}
		} finally {
			if (simpleWriter != null) {
				simpleWriter.close();
			}
		}
	}
	
}
