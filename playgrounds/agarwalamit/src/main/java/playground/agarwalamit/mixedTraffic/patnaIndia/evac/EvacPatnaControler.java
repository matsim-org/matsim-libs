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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class EvacPatnaControler {

	public static void main(String[] args) {

		String configFile ;
		LinkDynamics linkDynamics;
		String outDir;
		boolean isSeepModeStorageFree ;

		if(args.length==0){
			configFile = FileUtils.RUNS_SVN+"/patnaIndia/run109/1pct/input/evac_config.xml.gz";
			outDir = FileUtils.RUNS_SVN+"/patnaIndia/run109/1pct/withoutHoles/";
			linkDynamics = LinkDynamics.PassingQ;
			isSeepModeStorageFree = false;
		} else {
			configFile = args[0];
			outDir = args[1];
			linkDynamics = LinkDynamics.valueOf(args[2]);
			isSeepModeStorageFree = Boolean.valueOf(args[3]);
		}

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outDir);

		config.qsim().setLinkDynamics(linkDynamics);
		config.qsim().setSeepModeStorageFree(isSeepModeStorageFree);
		Collection<String> seepModes = Arrays.asList("bike");
		config.qsim().setSeepModes(seepModes );
		
		String outputDir ;
		if(linkDynamics.equals(LinkDynamics.SeepageQ)) {
			outputDir = config.controler().getOutputDirectory()+"/evac_"+linkDynamics.name()+"_"+seepModes.toString();
			if(isSeepModeStorageFree) outputDir = outputDir.concat("_noStorageCap/");
			else outputDir = outputDir.concat("/");
		} else outputDir = config.controler().getOutputDirectory()+"/evac_"+linkDynamics.name()+"/";
		
		config.controler().setOutputDirectory(outputDir);
		config.controler().setDumpDataAtEnd(true);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.vspExperimental().setWritingOutputEvents(true);

		Scenario sc = ScenarioUtils.loadScenario(config); 

		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		PatnaVehiclesGenerator.createAndAddVehiclesToScenario(sc, PatnaUtils.URBAN_MAIN_MODES);

		final Controler controler = new Controler(sc);

		final RandomizingTimeDistanceTravelDisutilityFactory builder_bike =  new RandomizingTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("bike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());

				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());					
			}
		});

		controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
			}
		});

		controler.run();
		
		new File(outputDir+"/analysis/").mkdir();
		String outputEventsFile = outputDir+"/output_events.xml.gz";
		// write some default analysis
		String userGroup = PatnaUserGroup.urban.toString();
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile);
		mtta.run();
		mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

		
	}
}