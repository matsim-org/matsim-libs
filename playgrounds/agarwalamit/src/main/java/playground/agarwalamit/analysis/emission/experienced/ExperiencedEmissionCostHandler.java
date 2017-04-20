/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.emission.experienced;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.*;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.EmissionCostHandler;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.IntervalHandler;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * Emission costs (air pollution exposure cost module is used).
 *
 * @author amit
 */

public class ExperiencedEmissionCostHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler, EmissionCostHandler{

	private static final Logger LOG = Logger.getLogger(ExperiencedEmissionCostHandler.class);

	private final Map<Id<Vehicle>, Double> vehicleId2ColdEmissCosts = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicleId2WarmEmissCosts = new HashMap<>();

	@Inject
	private EmissionResponsibilityCostModule emissionCostModule;
	@Inject(optional=true) private PersonFilter pf  ;

	public ExperiencedEmissionCostHandler(){}

	public ExperiencedEmissionCostHandler(final EmissionResponsibilityCostModule emissionCostModule, final PersonFilter pf) {
		this.emissionCostModule = emissionCostModule;
		this.pf = pf;
	}

	public static void main (String args []) {

		// munich CNE specific data
		
//		final Integer noOfXCells = 270;
//		final Integer noOfYCells = 208;
//		final double xMin = 4452550.;
//		final double xMax = 4479550.;
//		final double yMin = 5324955.;
//		final double yMax = 5345755.;
		
//		final Integer noOfXCells = 160;
//		final Integer noOfYCells = 120;
//		final double xMin = 4452550.25;
//		final double xMax = 4479483.33;
//		final double yMin = 5324955.00;
//		final double yMax = 5345696.81;
		
		// berlin CNE specific data
		
		final Integer noOfXCells = 677;
		final Integer noOfYCells = 446;
		final double xMin = 4565039.;
		final double xMax = 4632739.;
		final double yMin = 5801108.;
		final double yMax = 5845708.;

		final Double timeBinSize = 3600.;
		final int noOfTimeBins = 30;

		// munich
//		String dir = "/Users/amit/Documents/cluster/ils4/kaddoura/cne/munich/output/";
//		String outFile = "/Users/amit/Documents/cluster/ils4/agarwal/munich/airPolluationExposureCosts_cne.txt";

		// berlin
		String dir = "/Users/ihab/Desktop/ils4i/kaddoura/cne/berlin/output/";
		String outFile = dir +"airPolluationExposureCosts_cne.txt";
		
		// munich
//		String [] cases = {
//				"output_run0_muc_bc","output_run0b_muc_bc"
//				,"output_run1_muc_c_QBPV3","output_run1b_muc_c_QBPV3"
//				,"output_run2_muc_c_QBPV9","output_run2b_muc_c_QBPV9"
//				,"output_run3_muc_c_DecongestionPID","output_run3b_muc_c_DecongestionPID"
//				,"output_run4_muc_cne_DecongestionPID","output_run4b_muc_cne_DecongestionPID"
//				,"output_run5_muc_cne_QBPV3","output_run5b_muc_cne_QBPV3"
//				,"output_run6_muc_cne_QBPV9","output_run6b_muc_cne_QBPV9"
//				,"output_run7_muc_n","output_run7b_muc_n"
//				,"output_run8_muc_e","output_run8b_muc_e"
//		};
//		int [] its = {1000, 1500};

		// berlin
		String [] cases = {
				"output_run0_bln_bc_r"
				,"output_run1_bln_c_QBPV3_r"
				,"output_run2_bln_c_QBPV9_r"
				,"output_run3_bln_c_DecongestionPID_r"
				,"output_run4_bln_cne_DecongestionPID_r_new"
				,"output_run7_bln_n_r"
				,"output_run8_bln_e_r"
		};
		int [] its = {100};
		
		try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("case \t itNr \t costsInEur \t tollValuesEUR \n");

		for(String str : cases) {
				for(int itr : its) {
					String emissionEventsFile = dir + str + "/ITERS/it." + itr + "/" + itr + ".emission.events.xml.gz";
					String networkFile = dir+str+"/output_network.xml.gz";
					String configFile = dir+str+"/output_config.xml.gz";
					String eventsFile = dir + str + "/ITERS/it." + itr + "/" + itr + ".events.xml.gz";

					if(! new File(emissionEventsFile).exists() || ! new File(networkFile).exists() || ! new File(configFile).exists() || ! new File(eventsFile).exists() ) {
						continue;
					}

					double simulationEndtime = LoadMyScenarios.getSimulationEndTime(configFile);

					GridTools gt = new GridTools(LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
					IntervalHandler intervalHandler = new IntervalHandler(timeBinSize, simulationEndtime, gt);

					final Map<Id<Person>, Double> person2toll = new HashMap<>();
					EventsManager eventsManager = EventsUtils.createEventsManager();
					eventsManager.addHandler(intervalHandler);
					eventsManager.addHandler(new PersonMoneyEventHandler() {
						@Override
						public void handleEvent(PersonMoneyEvent event) {
							if(person2toll.containsKey(event.getPersonId())) {
								person2toll.put(event.getPersonId(), person2toll.get(event.getPersonId()) + event.getAmount());
							} else {
								person2toll.put(event.getPersonId(), event.getAmount());
							}
						}
						@Override
						public void reset(int iteration) {

						}
					});
					new MatsimEventsReader(eventsManager).readFile(eventsFile);

					ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
					rgt.resetAndcaluculateRelativeDurationFactors(intervalHandler.getDuration());

					EmissionsConfigGroup emissionsConfigGroup  = new EmissionsConfigGroup();
					emissionsConfigGroup.setConsideringCO2Costs(true);
					emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);

					EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(emissionsConfigGroup, rgt);
					ExperiencedEmissionCostHandler handler = new ExperiencedEmissionCostHandler(emissionCostModule, new MunichPersonFilter());

					EventsManager events = EventsUtils.createEventsManager();
					events.addHandler(handler);
					EmissionEventsReader reader = new EmissionEventsReader(events);
					reader.readFile(emissionEventsFile);

					handler.getUserGroup2TotalEmissionCosts().entrySet().forEach(e -> System.out.println(e.getKey()+"\t"+e.getValue()));
					writer.write(str+"\t"+itr+"\t"+MapUtils.doubleValueSum(handler.getUserGroup2TotalEmissionCosts())+"\t");

					writer.write(MapUtils.doubleValueSum(person2toll)+"\n");
				}
			}
			writer.close();
		} catch(IOException e) {
			throw new RuntimeException("Data is not written.");
		}
	}

	@Override
	public void reset(int iteration) {
		this.vehicleId2ColdEmissCosts.clear();
		this.vehicleId2WarmEmissCosts.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		double warmEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions(), event.getLinkId(), event.getTime());
		double amount2Pay =  warmEmissionCosts;

		if(this.vehicleId2WarmEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2WarmEmissCosts.get(vehicleId);
			this.vehicleId2WarmEmissCosts.put(vehicleId, nowCost+amount2Pay);
		} else {
			this.vehicleId2WarmEmissCosts.put(vehicleId, amount2Pay);
		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		double coldEmissionCosts = this.emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions(), event.getLinkId(), event.getTime());
		double amount2Pay =  coldEmissionCosts;

		if(this.vehicleId2ColdEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2ColdEmissCosts.get(vehicleId);
			this.vehicleId2ColdEmissCosts.put(vehicleId, nowCost+amount2Pay);
		} else {
			this.vehicleId2ColdEmissCosts.put(vehicleId, amount2Pay);
		}
	}

	public Map<Id<Person>, Double> getPersonId2ColdEmissionCosts() {
		final Map<Id<Person>, Double> personId2ColdEmissCosts =	this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> Id.createPersonId(entry.getKey().toString()), entry -> entry.getValue())
		);
		return personId2ColdEmissCosts;
	}

	public Map<Id<Person>, Double> getPersonId2WarmEmissionCosts() {
		final Map<Id<Person>, Double> personId2WarmEmissCosts =	this.vehicleId2WarmEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> Id.createPersonId(entry.getKey().toString()), entry -> entry.getValue())
		);
		return personId2WarmEmissCosts;
	}

	@Override
	public Map<Id<Person>, Double> getPersonId2TotalEmissionCosts() {
		return getPersonId2ColdEmissionCosts().entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() + this.getPersonId2WarmEmissionCosts().get(entry.getKey()))
		);
	}

	@Override
	public Map<Id<Vehicle>, Double> getVehicleId2TotalEmissionCosts(){
		return this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() + this.vehicleId2WarmEmissCosts.get(entry.getKey()))
		);
	}

	public Map<String, Double> getUserGroup2WarmEmissionCosts(){
		Map<String, Double> usrGrp2Cost = new HashMap<>();
		if (this.pf != null) {
			for (Map.Entry<Id<Person>, Double> entry : getPersonId2WarmEmissionCosts().entrySet()) {
				String ug = this.pf.getUserGroupAsStringFromPersonId(entry.getKey());
				usrGrp2Cost.put(ug,   usrGrp2Cost.containsKey(ug) ? entry.getValue() + usrGrp2Cost.get(ug) : entry.getValue());
			}
		} else {
			LOG.warn("The person filter is null, still, trying to get emission costs per user group. Returning emission costs for all persons.");
			usrGrp2Cost.put("AllPersons", MapUtils.doubleValueSum(this.vehicleId2WarmEmissCosts));
		}
		return usrGrp2Cost;
	}

	public Map<String, Double> getUserGroup2ColdEmissionCosts(){
		Map<String, Double> usrGrp2Cost = new HashMap<>();
		if(this.pf!=null) {
			for (Map.Entry<Id<Person>, Double> entry : getPersonId2ColdEmissionCosts().entrySet()) {
				String ug = this.pf.getUserGroupAsStringFromPersonId(entry.getKey());
				usrGrp2Cost.put(ug, usrGrp2Cost.containsKey(ug) ? entry.getValue() + usrGrp2Cost.get(ug) : entry.getValue());
			}
		} else {
			LOG.warn("The person filter is null, still, trying to get emission costs per user group. Returning emission costs for all persons.");
			usrGrp2Cost.put("AllPersons", MapUtils.doubleValueSum(this.vehicleId2ColdEmissCosts));
		}
		return usrGrp2Cost;
	}

	@Override
	public Map<String, Double> getUserGroup2TotalEmissionCosts(){
		return getUserGroup2ColdEmissionCosts().entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(),
						entry -> entry.getValue() + getUserGroup2WarmEmissionCosts().get(entry.getKey()))
		);
	}

	@Override
	public boolean isFiltering() {
		return ! (pf==null);
	}

	public Map<Id<Vehicle>, Double> getVehicleId2ColdEmissionCosts() {
		return this.vehicleId2ColdEmissCosts;
	}

	public Map<Id<Vehicle>, Double> getVehicleId2WarmEmissionCosts() {
		return this.vehicleId2WarmEmissCosts;
	}

}
