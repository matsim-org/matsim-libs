package org.matsim.contrib.cadyts.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/*package*/ class SimResultsContainerImpl implements SimResults<TransitStopFacility> {
	private static final long serialVersionUID = 1L;
	private CadytsPtOccupancyAnalyzer occupancyAnalyzer = null;
	private final double countsScaleFactor;
	private final int timeBinSize_s;

	SimResultsContainerImpl(final CadytsPtOccupancyAnalyzer oa, final double countsScaleFactor, int timeBinSize_s) {
		this.occupancyAnalyzer = oa;
		this.countsScaleFactor = countsScaleFactor;
		this.timeBinSize_s = timeBinSize_s;
	}

	@Override
	public double getSimValue(final TransitStopFacility stop, final int startTime_s, final int endTime_s, final TYPE type) { // stopFacility or link

		double retval = 0. ;
		switch ( type ) {
		case COUNT_VEH:
			retval = this.occupancyAnalyzer.getOccupancyVolumeForStopAndTime(stop.getId(), startTime_s) * this.countsScaleFactor ;
			break;
		case FLOW_VEH_H:
			int multiple = this.timeBinSize_s / 3600 ; // e.g. "3" when timeBinSize_s = 3*3600 = 10800
			retval = this.occupancyAnalyzer.getOccupancyVolumeForStopAndTime(stop.getId(), startTime_s) * this.countsScaleFactor / multiple ;
			break;
		}
		return retval ;
		
	}

	@Override
	public String toString() {
		final StringBuffer stringBuffer2 = new StringBuffer();
		final String STOPID = "stopId: ";
		final String VALUES = "; values:";
		final char TAB = '\t';
		final char RETURN = '\n';

		for (Id stopId : this.occupancyAnalyzer.getOccupancyStopIds()) { // Only occupancy!
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(STOPID);
			stringBuffer.append(stopId);
			stringBuffer.append(VALUES);

			boolean hasValues = false; // only prints stops with volumes > 0
			@SuppressWarnings("deprecation")
			int[] values = this.occupancyAnalyzer.getOccupancyVolumesForStop(stopId);

			for (int ii = 0; ii < values.length; ii++) {
				hasValues = hasValues || (values[ii] > 0);

				stringBuffer.append(TAB);
				stringBuffer.append(values[ii]);
			}
			stringBuffer.append(RETURN);
			if (hasValues)
				stringBuffer2.append(stringBuffer.toString());

		}
		return stringBuffer2.toString();
	}

}