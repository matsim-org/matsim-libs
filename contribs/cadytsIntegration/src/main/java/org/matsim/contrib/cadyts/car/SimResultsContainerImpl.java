package org.matsim.contrib.cadyts.car;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/*package*/ class SimResultsContainerImpl implements SimResults<Link> {
	private static final long serialVersionUID = 1L;
	private final PcuVolumesAnalyzer pcuVolumesAnalyzer;
	private final double countsScaleFactor;

	SimResultsContainerImpl(final PcuVolumesAnalyzer pcuVolumesAnalyzer, final double countsScaleFactor) {
		this.pcuVolumesAnalyzer = pcuVolumesAnalyzer;
		this.countsScaleFactor = countsScaleFactor;
	}

	@Override
	public double getSimValue(final Link link, final int startTime_s, final int endTime_s, final TYPE type) {

		Id<Link> linkId = link.getId();
		double[] values = pcuVolumesAnalyzer.getPcuVolumesForLink(linkId);

		if (values == null) {
			return 0;
		}

		// Assuming analyzer bins are consistent with request (usually hourly or matching config)
		// Usually Cadyts configures bin size to 3600s.
		int binSize = 3600; // This should ideally match the analyzer's bin size
		int startBin = startTime_s / binSize;
		int endBin = (endTime_s - 1) / binSize;

		double sum = 0.0;
		for (int i = startBin; i <= endBin; i++) {
			if (i >= 0 && i < values.length) {
				sum += values[i];
			}
		}

		switch(type){
			case COUNT_VEH: // Cadyts expects "Count", we provide "PCU Count"
				return sum * this.countsScaleFactor;
			case FLOW_VEH_H:
				return 3600.0 * sum / (endTime_s - startTime_s) * this.countsScaleFactor;
			default:
				throw new RuntimeException("count type not implemented");
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		final String LINKID = "linkId: ";
		final String VALUES = "; values:";
		final char TAB = '\t';
		final char RETURN = '\n';

		for (Id<Link> linkId : this.pcuVolumesAnalyzer.getLinkIds()) {
			StringBuilder linkSb = new StringBuilder();
			linkSb.append(LINKID);
			linkSb.append(linkId);
			linkSb.append(VALUES);

			boolean hasValues = false; // only prints links with volumes > 0
			double[] values = this.pcuVolumesAnalyzer.getPcuVolumesForLink(linkId);

			if (values != null) {
				for (double value : values) {
					hasValues = hasValues || (value > 0);
					linkSb.append(TAB);
					linkSb.append(value);
				}
			}
			linkSb.append(RETURN);
			if (hasValues) {
				sb.append(linkSb);
			}
		}
		return sb.toString();
	}

}
