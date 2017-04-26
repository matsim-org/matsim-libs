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
package playground.agarwalamit.congestionPricing.testExamples.pricing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.analysis.tripTime.LinkTravelTimeCalculator;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

class PricingTestAnalyser {

	private final String outputDir = "../../../repos/shared-svn/papers/2014/congestionInternalization/implV4/hEART/testExample/";

	public static void main(String[] args) {
		new PricingTestAnalyser().run();
	}

	void run (){
		String [] congestionImpls = {"noToll","implV3","implV4","implV6"};

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/output/analysis/pricingComparison.txt");
		try {
			writer.write("congestionImpl \t linkId \t linkCount \t noOfTollPayers \t experiencedDelayInHr \t causedDelayInHr \n");

			for (String str :congestionImpls) {
				writer.write(analyseAndWriteData(str));
				writer.newLine();

				writeLinkTravelTimes(str);
			}

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	private void writeLinkTravelTimes (String congestionImpl){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(outputDir+"/input/input_plans.xml.gz", outputDir+"/input/input_network.xml", outputDir+"/input/input_config.xml.gz");

		int lastIt = sc.getConfig().controler().getLastIteration();
		String eventsFile = outputDir+"/output/"+congestionImpl+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";

		LinkTravelTimeCalculator lttc = new LinkTravelTimeCalculator(eventsFile);
		lttc.preProcessData();
		Map<Id<Link>, Map<Id<Person>, List<Double>>> link2Person2TravelTime = lttc.getLink2Person2TravelTime();

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/output/"+congestionImpl+"/ITERS/it."+lastIt+"/"+lastIt+".travelTime.txt");
		try {
			writer.write("linkId \t personCount \t totalTravelTimeSec \t avgTravelTimeSec \n");

			for(Id<Link> linkId : link2Person2TravelTime.keySet()){
				double sum = 0;
				int count = 0;
				for(Id<Person> personId : link2Person2TravelTime.get(linkId).keySet()){
					for(double d : link2Person2TravelTime.get(linkId).get(personId)){
						sum += d;
						count ++;
					}
				}
				writer.write(linkId+"\t"+count+"\t"+sum+"\t"+sum/count+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}

	}

	private String analyseAndWriteData (String congestionImpl) {

		String out = "";
		DecimalFormat df = new DecimalFormat();

		Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(outputDir+"/input/input_plans.xml.gz", outputDir+"/input/input_network.xml", outputDir+"/input/input_config.xml.gz");

		int lastIt = sc.getConfig().controler().getLastIteration();
		String eventsFile = outputDir+"/output/"+congestionImpl+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";

		ExperiencedDelayAnalyzer eda = new ExperiencedDelayAnalyzer(eventsFile, sc, 1);
		eda.run();


		CausedDelayAnalyzer cda = new CausedDelayAnalyzer(eventsFile, sc, 1);
		cda.run();

		Map<Id<Link>, Set<Id<Person>>> tollPayersCount = cda.getTimeBin2Link2CausingPersons().get(sc.getConfig().qsim().getEndTime());
		Map<Id<Link>, Double> causedDelays =  cda.getTimeBin2LinkId2Delay().get(sc.getConfig().qsim().getEndTime());
		Map<Id<Link>, Integer> linkCount = eda.getTimeBin2LinkLeaveCount().get(sc.getConfig().qsim().getEndTime());
		Map<Id<Link>, Double> expDelays = eda.getTimeBin2LinkId2Delay().get(sc.getConfig().qsim().getEndTime());

		for(Id<Link> l : tollPayersCount.keySet()){
			out= out + congestionImpl+"\t" + (l+"\t"+linkCount.get(l)+"\t"+tollPayersCount.get(l).size()+"\t"+df.format(expDelays.get(l)/3600.)+"\t"+df.format(causedDelays.get(l)/3600.))+"\n";
		}

		return out;
	}
}
