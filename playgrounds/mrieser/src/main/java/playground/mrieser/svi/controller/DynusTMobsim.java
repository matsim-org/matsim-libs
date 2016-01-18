/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.svi.data.DynamicODMatrix;
import playground.mrieser.svi.data.DynusTDynamicODDemandWriter;
import playground.mrieser.svi.data.analysis.CalculateLinkStatsFromVehTrajectories;
import playground.mrieser.svi.data.analysis.CalculateLinkTravelTimesFromVehTrajectories;
import playground.mrieser.svi.data.analysis.CalculateTravelTimeMatrixFromVehTrajectories;
import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;
import playground.mrieser.svi.data.vehtrajectories.CsvReformatter;
import playground.mrieser.svi.data.vehtrajectories.Extractor;
import playground.mrieser.svi.data.vehtrajectories.MultipleVehicleTrajectoryHandler;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoriesReader;
import playground.mrieser.svi.replanning.DynamicODDemandCollector;
import playground.mrieser.svi.replanning.MultimodalDynamicODDemandCollector;

/**
 * @author mrieser
 */
public class DynusTMobsim implements Mobsim {

	private final static Logger log = Logger.getLogger(DynusTMobsim.class);

	private final DynusTConfig dc;
	private final Scenario scenario;
	private final DynamicTravelTimeMatrix ttMatrix;
	private final Network dynusTnet;
	private final MatsimServices controler;
	private final int iteration;

	public DynusTMobsim(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final Scenario sc, final EventsManager eventsManager,
			final Network dynusTnet, final MatsimServices controler, final int iteration) {
		this.dc = dc;
		this.scenario = sc;
		this.ttMatrix = ttMatrix;
		this.dynusTnet = dynusTnet;
		this.controler = controler;
		this.iteration = iteration;
	}

	@Override
	public void run() {
		// prepare matrix
		log.info("collect demand for Dynus-T");
		DynamicODMatrix odm = new DynamicODMatrix(this.dc.getTimeBinSize_min()*60, 24*60*60);
		DynamicODDemandCollector collector = new DynamicODDemandCollector(odm, this.dc.getActToZoneMapping());

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			collector.run(plan);
		}
		log.info("Number of trips handed over to DynusT: " + collector.getCounter());
		printModeShares(collector);
		
		log.info("Collect demand per mode");
		MultimodalDynamicODDemandCollector mmCollector = new MultimodalDynamicODDemandCollector(3600, 24*3600.0, this.dc.getActToZoneMapping());
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			mmCollector.run(plan);
		}
		
		log.info("Write demand per mode");
		exportDemand(mmCollector);
		
		log.info("write marginal sums");
		exportMarginalSums(odm, this.controler.getControlerIO().getIterationFilename(this.iteration, "nachfrageRandsummen.txt"));
		
		log.info("write demand for Dynus-T with factor " + this.dc.getDemandFactor());
		DynusTDynamicODDemandWriter writer = new DynusTDynamicODDemandWriter(odm, this.dc.getZoneIdToIndexMapping());
		writer.setMultiplyFactor(this.dc.getDemandFactor());
		writer.writeTo(this.dc.getOutputDirectory() + "/demand.dat");

		// run DynusT
		log.info("run Dynus-T");
		DynusTExe exe = new DynusTExe(this.dc.getDynusTDirectory(), this.dc.getModelDirectory(), this.dc.getOutputDirectory());
		exe.runDynusT(true);

		// read in data, convert it somehow to score the plans
		log.info("read in Vehicle Trajectories from DynusT");
		String vehTrajFilename = this.dc.getOutputDirectory() + "/VehTrajectory.dat";
		
		DynamicTravelTimeMatrix hourlyTravelTimesMatrix = new DynamicTravelTimeMatrix(3600, 24*3600);
		
		MultipleVehicleTrajectoryHandler multiHandler = new MultipleVehicleTrajectoryHandler();
		CalculateTravelTimeMatrixFromVehTrajectories ttmCalc = new CalculateTravelTimeMatrixFromVehTrajectories(this.ttMatrix);
		multiHandler.addTrajectoryHandler(ttmCalc);
		CalculateTravelTimeMatrixFromVehTrajectories hourlyTtmCalc = new CalculateTravelTimeMatrixFromVehTrajectories(hourlyTravelTimesMatrix);
		multiHandler.addTrajectoryHandler(hourlyTtmCalc);
		TravelTimeCalculator ttc = new TravelTimeCalculator(this.dynusTnet, this.scenario.getConfig().travelTimeCalculator());
		CalculateLinkTravelTimesFromVehTrajectories lttCalc = new CalculateLinkTravelTimesFromVehTrajectories(ttc, this.dynusTnet);
		multiHandler.addTrajectoryHandler(lttCalc);
		CalculateLinkStatsFromVehTrajectories linkStats = new CalculateLinkStatsFromVehTrajectories(this.dynusTnet);
		multiHandler.addTrajectoryHandler(linkStats);
		
		new VehicleTrajectoriesReader(multiHandler, this.dc.getZoneIdToIndexMapping()).readFile(vehTrajFilename);

		this.dc.setTravelTimeCalculator(ttc);
		linkStats.writeLinkVolumesToFile(this.controler.getControlerIO().getIterationFilename(this.iteration, "dynust_linkVolumes.txt"));
		linkStats.writeLinkTravelTimesToFile(this.controler.getControlerIO().getIterationFilename(this.iteration, "dynust_linkTravelTimes.txt"));
		linkStats.writeLinkTravelSpeedsToFile(this.controler.getControlerIO().getIterationFilename(this.iteration, "dynust_linkTravelSpeeds.txt"));
		exportTravelTimes(hourlyTravelTimesMatrix, hourlyTtmCalc.getZoneIds(), this.controler.getControlerIO().getIterationFilename(this.iteration, "dynust_odTravelTimes.txt"));
		extractVehTrajectories(vehTrajFilename);
		convertVehTrajectories(vehTrajFilename);
	}

	private void printModeShares(final DynamicODDemandCollector collector) {
		log.info("Mode share statistics:");
		int sum = 0;
		for (Map.Entry<String, Integer> e : collector.getModeCounts().entrySet()) {
			sum += e.getValue().intValue();
		}
		for (Map.Entry<String, Integer> e : collector.getModeCounts().entrySet()) {
			log.info("   # trips with mode " + e.getKey() + " = " + e.getValue() + " (" + ((e.getValue().doubleValue() / (double) sum) * 100) + "%)");
		}		
	}
	
	private void exportMarginalSums(final DynamicODMatrix matrix, final String filename) {
		Map<String, Integer> origins = new HashMap<String, Integer>();
		Map<String, Integer> destinations = new HashMap<String, Integer>();
		
		for (int i = 0; i < matrix.getNOfBins(); i++) {
			Map<String, Map<String, Integer>> timeMatrix = matrix.getMatrixForTimeBin(i);
			
			for (Map.Entry<String, Map<String, Integer>> fromZone : timeMatrix.entrySet()) {
				String fromZoneId = fromZone.getKey();
				for (Map.Entry<String, Integer> toZone : fromZone.getValue().entrySet()) {
					String toZoneId = toZone.getKey();
					Integer volume = toZone.getValue();
					
					Integer origVol = origins.get(fromZoneId);
					if (origVol == null) {
						origins.put(fromZoneId, volume);
					} else {
						origins.put(fromZoneId, volume.intValue() + origVol.intValue());
					}

					Integer destVol = destinations.get(toZoneId);
					if (destVol == null) {
						destinations.put(toZoneId, volume);
					} else {
						destinations.put(toZoneId, volume.intValue() + destVol.intValue());
					}
				}
			}
		}

		Set<String> zoneIds = new HashSet<String>(origins.keySet());
		zoneIds.addAll(destinations.keySet());
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);

		try {
			writer.write("ZONE\tQuellverkehr\tZielverkehr" + IOUtils.NATIVE_NEWLINE);
			
			for (String id : zoneIds) {
				writer.write(id + "\t");
				Integer vol = origins.get(id);
				if (vol != null) {
					writer.write(vol.toString());
				}
				writer.write("\t");
				vol = destinations.get(id);
				if (vol != null) {
					writer.write(vol.toString());
				}
				writer.write(IOUtils.NATIVE_NEWLINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void exportDemand(final MultimodalDynamicODDemandCollector collector) {
		Map<String, DynamicODMatrix> modeDemand = collector.getDemand();
		
		try {
			for (Map.Entry<String, DynamicODMatrix> e : modeDemand.entrySet()) {
				String mode = e.getKey();
				DynamicODMatrix timeMatrices = e.getValue();
				
				String filename = this.controler.getControlerIO().getIterationFilename(this.iteration, "demand_" + mode + ".txt");
				BufferedWriter writer = IOUtils.getBufferedWriter(filename);
				
				int binCount = timeMatrices.getNOfBins();
				int binSize = timeMatrices.getBinSize();
				
				Set<String> fromZoneIds = new HashSet<String>();
				Set<String> toZoneIds = new HashSet<String>();
				
				for (int i = 0; i < binCount; i++) {
					Map<String, Map<String, Integer>> odm = timeMatrices.getMatrixForTimeBin(i);
					if (odm != null) {
						fromZoneIds.addAll(odm.keySet());
						for (Map<String, Integer> ds : odm.values()) {
							toZoneIds.addAll(ds.keySet());
						}
					}
				}
				
				fromZoneIds.remove(null);
				toZoneIds.remove(null);
				
				// header
				writer.write("VON_ZONE\tNACH_ZONE");
				for (int i = 0; i < binCount; i++) {
					writer.write("\t");
					writer.write(Time.writeTime(i * binSize, Time.TIMEFORMAT_HHMM, '_'));
				}
				writer.write(IOUtils.NATIVE_NEWLINE);
				
				// content
				for (String fromZoneId : fromZoneIds) {
					for (String toZoneId : toZoneIds) {
						writer.write(fromZoneId);
						writer.write('\t');
						writer.write(toZoneId);
						for (int i = 0; i < binCount; i++) {
							writer.write('\t');
							Map<String, Map<String, Integer>> odm = timeMatrices.getMatrixForTimeBin(i);
							if (odm == null) {
								writer.write('0');
							} else {
								Map<String, Integer> rows = odm.get(fromZoneId);
								if (rows == null) {
									writer.write('0');
								} else {
									Integer val = rows.get(toZoneId);
									if (val == null) {
										writer.write('0');
									} else {
										writer.write(val.toString());
									}
								}
							}
						}
						writer.write(IOUtils.NATIVE_NEWLINE);
					}
				}
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void exportTravelTimes(final DynamicTravelTimeMatrix matrix, final Set<String> zoneIds, final String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			
			int binCount = matrix.getNOfBins();
			int binSize = matrix.getBinSize();
			
			zoneIds.remove(null);
			
			// header
			writer.write("VON_ZONE\tNACH_ZONE");
			for (int i = 0; i < binCount; i++) {
				writer.write("\t");
				writer.write(Time.writeTime(i * binSize, Time.TIMEFORMAT_HHMM, '_'));
			}
			writer.write(IOUtils.NATIVE_NEWLINE);
			
			// content
			for (String fromZoneId : zoneIds) {
				for (String toZoneId : zoneIds) {
					writer.write(fromZoneId);
					writer.write('\t');
					writer.write(toZoneId);
					for (int i = 0; i < binCount; i++) {
						double time = i * binSize;
						writer.write('\t');
						double tt = matrix.getAverageTravelTimeWithUnknown(time, fromZoneId, toZoneId);
						writer.write(Double.toString(tt));
					}
					writer.write(IOUtils.NATIVE_NEWLINE);
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void extractVehTrajectories(final String vehTrajFilename) {
		for (Tuple<Double, Double> t : this.dc.getVehTrajectoryExtracts()) {
			String filename = "VehTrajectories_" + Time.writeTime(t.getFirst(), Time.TIMEFORMAT_HHMM, '_') + "-" + Time.writeTime(t.getSecond(), Time.TIMEFORMAT_HHMM, '_') + ".dat";
			String outputFile = this.controler.getControlerIO().getIterationFilename(this.iteration, filename);
			Extractor.filterVehTrajectory(vehTrajFilename, t.getFirst() / 60.0, t.getSecond() / 60.0, outputFile); // convert times from seconds to minutes
		}
	}

	private void convertVehTrajectories(final String vehTrajFilename) {
		try {
			CsvReformatter csv = new CsvReformatter(this.dc.getZoneIdToIndexMapping(), this.controler.getControlerIO().getIterationFilename(this.iteration, "vehTrajectories.csv"));
			new VehicleTrajectoriesReader(csv, this.dc.getZoneIdToIndexMapping()).readFile(vehTrajFilename);
			csv.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
