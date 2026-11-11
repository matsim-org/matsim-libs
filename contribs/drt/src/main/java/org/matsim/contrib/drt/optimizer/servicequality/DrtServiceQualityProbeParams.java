/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */
package org.matsim.contrib.drt.optimizer.servicequality;

import org.matsim.core.config.ReflectiveConfigGroup;

/** Configuration for the side-effect-free DRT service-quality probe. */
public class DrtServiceQualityProbeParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "serviceQualityProbe";

	public enum SpatialResolution {STOP_TO_STOP, ZONE_TO_ZONE}

	public DrtServiceQualityProbeParams() {
		super(SET_NAME);
	}

	private boolean writeServiceQualityProbes = false;
	private String serviceQualityProbeTimes = "";
	private String serviceQualityProbeOutputFile = "drt_service_quality_probes.csv.gz";
	private String serviceQualityProbeStopPairInputFiles = "";
	private SpatialResolution serviceQualityProbeSpatialResolution = SpatialResolution.STOP_TO_STOP;
	private double serviceQualityProbeZoneCellSize = Double.NaN;

	@StringGetter("writeServiceQualityProbes")
	public boolean isWriteServiceQualityProbes() { return writeServiceQualityProbes; }

	@StringSetter("writeServiceQualityProbes")
	public void setWriteServiceQualityProbes(boolean value) { writeServiceQualityProbes = value; }

	@StringGetter("serviceQualityProbeTimes")
	public String getServiceQualityProbeTimes() { return serviceQualityProbeTimes; }

	@StringSetter("serviceQualityProbeTimes")
	public void setServiceQualityProbeTimes(String value) { serviceQualityProbeTimes = value; }

	@StringGetter("serviceQualityProbeOutputFile")
	public String getServiceQualityProbeOutputFile() { return serviceQualityProbeOutputFile; }

	@StringSetter("serviceQualityProbeOutputFile")
	public void setServiceQualityProbeOutputFile(String value) { serviceQualityProbeOutputFile = value; }

	@StringGetter("serviceQualityProbeStopPairInputFiles")
	public String getServiceQualityProbeStopPairInputFiles() { return serviceQualityProbeStopPairInputFiles; }

	@StringSetter("serviceQualityProbeStopPairInputFiles")
	public void setServiceQualityProbeStopPairInputFiles(String value) { serviceQualityProbeStopPairInputFiles = value; }

	@StringGetter("serviceQualityProbeSpatialResolution")
	public SpatialResolution getServiceQualityProbeSpatialResolution() { return serviceQualityProbeSpatialResolution; }

	@StringSetter("serviceQualityProbeSpatialResolution")
	public void setServiceQualityProbeSpatialResolution(SpatialResolution value) { serviceQualityProbeSpatialResolution = value; }

	@StringGetter("serviceQualityProbeZoneCellSize")
	public double getServiceQualityProbeZoneCellSize() { return serviceQualityProbeZoneCellSize; }

	@StringSetter("serviceQualityProbeZoneCellSize")
	public void setServiceQualityProbeZoneCellSize(double value) { serviceQualityProbeZoneCellSize = value; }

	public boolean isEnabled() {
		return writeServiceQualityProbes && serviceQualityProbeTimes != null && !serviceQualityProbeTimes.isBlank();
	}
}
