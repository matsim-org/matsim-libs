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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory.QueueWithBufferType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class EvacPatnaControler {

	public static void main(String[] args) {

		String configFile ;
		boolean  isUsingSeepage;
		String outDir;
		boolean congestionPricing;

		if(args.length==0){
			configFile = "../../../repos/runs-svn/patnaIndia/run105/input/patna_evac_config.xml.gz";
			isUsingSeepage = false;
			outDir = "../../../repos/runs-svn/patnaIndia/run105/100pct/";
			congestionPricing = true;
		} else {
			configFile = args[0];
			isUsingSeepage = Boolean.valueOf(args[1]);
			outDir = args[2];
			congestionPricing = Boolean.valueOf(args[3]);
		}

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outDir);

		if(congestionPricing) config.controler().setOutputDirectory(config.controler().getOutputDirectory()+"/congestionPricing/");
		
		if(isUsingSeepage){	
			config.setParam("seepage", "isSeepageAllowed", "true");
			config.setParam("seepage", "seepMode", "bike");
			config.setParam("seepage", "isSeepModeStorageFree", "false");
			String outputDir = config.controler().getOutputDirectory()+"/evac_seepage/";
			config.controler().setOutputDirectory(outputDir);
		} else {
			String outputDir = config.controler().getOutputDirectory()+"/evac_passing/";
			config.controler().setOutputDirectory(outputDir);
		}

		Scenario sc = ScenarioUtils.loadScenario(config); 

		sc.getConfig().qsim().setUseDefaultVehicles(false);
		((ScenarioImpl) sc).createVehicleContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("car"));
		car.setPcuEquivalents(1.0);
		modesType.put("car", car);
		sc.getVehicles().addVehicleType(car);

		VehicleType motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorbike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("motorbike"));
		motorbike.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorbike);
		sc.getVehicles().addVehicleType(motorbike);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("bike"));
		bike.setPcuEquivalents(0.25);
		modesType.put("bike", bike);
		sc.getVehicles().addVehicleType(bike);

		for(Person p:sc.getPopulation().getPersons().values()){
			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			String travelMode = null;
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					travelMode = ((Leg)pe).getMode();
					break;
				}
			}
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modesType.get(travelMode));
			sc.getVehicles().addVehicle(vehicle);
		}

		final Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.setDumpDataAtEnd(true);

		if(isUsingSeepage){
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new SeepageMobsimfactory(QueueWithBufferType.amit).createMobsim(controler.getScenario(), controler.getEvents());
						}
					});
				}
			});
		}
		
		if(congestionPricing) {
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(),tollHandler, new CongestionHandlerImplV6(controler.getEvents(), (ScenarioImpl)controler.getScenario()) ));
		}
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl)controler.getScenario()));
		controler.run();

	}
}
