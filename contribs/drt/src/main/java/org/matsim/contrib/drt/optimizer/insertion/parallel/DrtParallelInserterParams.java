/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.parallel;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Steffen Axer
 */
public class DrtParallelInserterParams extends ReflectiveConfigGroup {

	public DrtParallelInserterParams() {
		super(SET_NAME);
	}

	public enum VehiclesPartitioner {
		ReplicatingVehicleEntryPartitioner, RoundRobinVehicleEntryPartitioner,
		ShiftingRoundRobinVehicleEntryPartitioner
	}

	public enum RequestsPartitioner {RoundRobinRequestsPartitioner, LoadAwareRoundRobinRequestsPartitioner}

	public enum ServiceQualityProbeSpatialResolution {STOP_TO_STOP, ZONE_TO_ZONE}

	public static final String SET_NAME = "parallelInserter";

	@Comment("Time window (in seconds) for collecting incoming requests before processing begins.")
	private double collectionPeriod = 15.0;

	@Comment("Maximum number of conflict resolution iterations allowed. " +
		"Each additional iteration may resolve conflicts where multiple partitions attempt to use the same vehicle.")
	private int maxIterations = 2;

	@Comment("Maximum number of partitions. Each partition handles a subset of requests and vehicles. " +
		"Note: Each partition requires a separate insertion search instance. " +
		"See also: insertionSearchThreadsPerWorker.")
	private int maxPartitions = 4;

	@Comment("Number of insertion search threads allocated per worker.")
	private int insertionSearchThreadsPerWorker = 4;

	@StringGetter("logThreadActivity")
	public boolean isLogThreadActivity() {
		return logThreadActivity;
	}

	@StringSetter("logThreadActivity")
	public void setLogThreadActivity(boolean logThreadActivity) {
		this.logThreadActivity = logThreadActivity;
	}

	@Comment("Enable/Disable thread activity statistics. Note: Disabled by default to improve performance and save memory.")
	private boolean logThreadActivity = false;

	@StringGetter("logPerformanceStats")
	public boolean isLogPerformanceStats() {
		return logPerformanceStats;
	}

	@StringSetter("logPerformanceStats")
	public void setLogPerformanceStats(boolean logPerformanceStats) {
		this.logPerformanceStats = logPerformanceStats;
	}

	@Comment("Enable/Disable detailed performance statistics including worker utilization, conflict resolution timing, and load balancing metrics. Disabled by default.")
	private boolean logPerformanceStats = false;

	@Comment("Enable/Disable service quality probing. If enabled, the parallel inserter estimates DRT service quality for all stop-to-stop pairs at the configured probe times in the last iteration only. Disabled by default.")
	private boolean writeServiceQualityProbes = false;

	@Comment("Comma-separated list of simulation times in seconds for service quality probing, e.g. '28800,32400,36000'.")
	private String serviceQualityProbeTimes = "";

	@Comment("Output file name for service quality probes. The file is written to the last iteration output directory.")
	private String serviceQualityProbeOutputFile = "drt_service_quality_probes.csv.gz";

	@Comment("Optional comma-separated stop-pair CSV/CSV.GZ files produced by accessibility. In STOP_TO_STOP mode, only their unique directed pairs are probed. If unset, all stop pairs are probed.")
	private String serviceQualityProbeStopPairInputFiles = "";

	@Comment("Spatial resolution for service quality probing. STOP_TO_STOP estimates all DRT stop pairs. ZONE_TO_ZONE estimates one representative link-to-link request for each travel-time-matrix zone pair.")
	private ServiceQualityProbeSpatialResolution serviceQualityProbeSpatialResolution = ServiceQualityProbeSpatialResolution.STOP_TO_STOP;

	@Comment("Optional square-grid cell size for ZONE_TO_ZONE service quality probing. If unset, the DVRP travel-time-matrix zone system is used.")
	private double serviceQualityProbeZoneCellSize = Double.NaN;

	@StringGetter("writeServiceQualityProbes")
	public boolean isWriteServiceQualityProbes() {
		return writeServiceQualityProbes;
	}

	@StringSetter("writeServiceQualityProbes")
	public void setWriteServiceQualityProbes(boolean writeServiceQualityProbes) {
		this.writeServiceQualityProbes = writeServiceQualityProbes;
	}

	@StringGetter("serviceQualityProbeTimes")
	public String getServiceQualityProbeTimes() {
		return serviceQualityProbeTimes;
	}

	@StringSetter("serviceQualityProbeTimes")
	public void setServiceQualityProbeTimes(String serviceQualityProbeTimes) {
		this.serviceQualityProbeTimes = serviceQualityProbeTimes;
	}

	@StringGetter("serviceQualityProbeOutputFile")
	public String getServiceQualityProbeOutputFile() {
		return serviceQualityProbeOutputFile;
	}

	@StringSetter("serviceQualityProbeOutputFile")
	public void setServiceQualityProbeOutputFile(String serviceQualityProbeOutputFile) {
		this.serviceQualityProbeOutputFile = serviceQualityProbeOutputFile;
	}

	@StringGetter("serviceQualityProbeStopPairInputFiles")
	public String getServiceQualityProbeStopPairInputFiles() {
		return serviceQualityProbeStopPairInputFiles;
	}

	@StringSetter("serviceQualityProbeStopPairInputFiles")
	public void setServiceQualityProbeStopPairInputFiles(String serviceQualityProbeStopPairInputFiles) {
		this.serviceQualityProbeStopPairInputFiles = serviceQualityProbeStopPairInputFiles;
	}

	@StringGetter("serviceQualityProbeSpatialResolution")
	public ServiceQualityProbeSpatialResolution getServiceQualityProbeSpatialResolution() {
		return serviceQualityProbeSpatialResolution;
	}

	@StringSetter("serviceQualityProbeSpatialResolution")
	public void setServiceQualityProbeSpatialResolution(ServiceQualityProbeSpatialResolution serviceQualityProbeSpatialResolution) {
		this.serviceQualityProbeSpatialResolution = serviceQualityProbeSpatialResolution;
	}

	@StringGetter("serviceQualityProbeZoneCellSize")
	public double getServiceQualityProbeZoneCellSize() {
		return serviceQualityProbeZoneCellSize;
	}

	@StringSetter("serviceQualityProbeZoneCellSize")
	public void setServiceQualityProbeZoneCellSize(double serviceQualityProbeZoneCellSize) {
		this.serviceQualityProbeZoneCellSize = serviceQualityProbeZoneCellSize;
	}

	@StringGetter("vehiclesPartitioner")
	public VehiclesPartitioner getVehiclesPartitioner() {
		return vehiclesPartitioner;
	}

	@StringSetter("vehiclesPartitioner")
	public void setVehiclesPartitioner(VehiclesPartitioner vehiclesPartitioner) {
		this.vehiclesPartitioner = vehiclesPartitioner;
	}

	@StringGetter("requestsPartitioner")
	public RequestsPartitioner getRequestsPartitioner() {
		return requestsPartitioner;
	}

	@StringSetter("requestsPartitioner")
	public void setRequestsPartitioner(RequestsPartitioner requestPartitioner) {
		this.requestsPartitioner = requestPartitioner;
	}

	@NotNull
	VehiclesPartitioner vehiclesPartitioner = VehiclesPartitioner.ShiftingRoundRobinVehicleEntryPartitioner;

	@NotNull
	RequestsPartitioner requestsPartitioner = RequestsPartitioner.LoadAwareRoundRobinRequestsPartitioner;

	@StringGetter("collectionPeriod")
	public double getCollectionPeriod() {
		return collectionPeriod;
	}

	@StringSetter("collectionPeriod")
	public void setCollectionPeriod(double collectionPeriod) {
		this.collectionPeriod = collectionPeriod;
	}

	@StringGetter("maxIterations")
	public int getMaxIterations() {
		return maxIterations;
	}

	@StringSetter("maxIterations")
	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	@StringGetter("maxPartitions")
	public int getMaxPartitions() {
		return maxPartitions;
	}

	@StringSetter("maxPartitions")
	public void setMaxPartitions(int maxPartitions) {
		this.maxPartitions = maxPartitions;
	}

	@StringGetter("insertionSearchThreadsPerWorker")
	public int getInsertionSearchThreadsPerWorker() {
		return insertionSearchThreadsPerWorker;
	}

	@StringSetter("insertionSearchThreadsPerWorker")
	public void setInsertionSearchThreadsPerWorker(int insertionSearchThreadsPerWorker) {
		this.insertionSearchThreadsPerWorker = insertionSearchThreadsPerWorker;
	}
}
