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
package playground.agarwalamit.analysis.Toll;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.munich.ExtendedPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * Idea is to write (1) total tolled persons (2) total tolled trips (3) total tolled links for each time bin and for each user group (if applicable).
 * @author amit
 */

public class TollCounterInfoWriter extends AbstractAnalyisModule {

	public TollCounterInfoWriter(String eventsFile, Scenario sc, int noOfTimeBins, boolean isSortingForMunich) {
		super(TollCounterInfoWriter.class.getSimpleName());
		this.eventsFile = eventsFile;
		this.sc = sc;
		this.noOfTimeBins = noOfTimeBins;
		this.isSortingForMunich = isSortingForMunich;
	}

	private String eventsFile;
	private Scenario sc;
	private int noOfTimeBins;
	private boolean isSortingForMunich;
	private CausedDelayAnalyzer cda;
	private ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private final String suffixForSoring = "_sorted";

	private SortedMap<Double, SortedMap<UserGroup, Integer>> userGroup2TollPayers = new TreeMap<Double, SortedMap<UserGroup,Integer>>();
	private SortedMap<Double,SortedMap<UserGroup, Integer>> userGroup2TolledTrips = new TreeMap<Double, SortedMap<UserGroup,Integer>>();
	private SortedMap<Double,Integer> timeBin2TolledLinks = new TreeMap<Double,Integer>();

	public static void main(String[] args) {
		
		String congestionImpl = "implV4";
		String outDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run12/policies/"+congestionImpl+"/";
		String eventsFile = outDir+"/ITERS/it.1500/1500.events.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outDir);
		TollCounterInfoWriter tcia = new TollCounterInfoWriter(eventsFile, sc, 30, true);
		tcia.preProcessData();
		tcia.postProcessData();
		tcia.writeResults(outDir+"/analysis/");
		
	}
	
	
	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		this.cda = new CausedDelayAnalyzer(this.eventsFile, this.sc, this.noOfTimeBins,this.isSortingForMunich);
		this.cda.run();
	}

	@Override
	public void postProcessData() {
		SortedMap<Double, Map<Id<Person>, Double>>  timeBin2PersonInfo = this.cda.getTimeBin2CausingPersonId2Delay();

		// initialize
		for(Double d : timeBin2PersonInfo.keySet()){
			SortedMap<UserGroup, Integer> usrGrp2Cnt = new TreeMap<UserGroup, Integer>();
			SortedMap<UserGroup, Integer> usrGrp2Cnt2 = new TreeMap<UserGroup, Integer>();
			for(UserGroup ug:UserGroup.values()){
				usrGrp2Cnt.put(ug, 0);
				usrGrp2Cnt2.put(ug, 0);
			}
			this.userGroup2TollPayers.put(d, usrGrp2Cnt);
			this.userGroup2TolledTrips.put(d, usrGrp2Cnt2);
			this.timeBin2TolledLinks.put(d, 0);
		}

		//timeBin2UserGrp2TolledPerson
		for(Double d : timeBin2PersonInfo.keySet()){
			SortedMap<UserGroup,Integer> usrGrp2Person = this.userGroup2TollPayers.get(d);
			for(Id<Person> personId : timeBin2PersonInfo.get(d).keySet()){
				if(timeBin2PersonInfo.get(d).get(personId)==0.) continue;
				UserGroup ug = this.pf.getUserGroupFromPersonId(personId);
				usrGrp2Person.put(ug, usrGrp2Person.get(ug)+1);
			}
		}

		//timeBin2UserGrp2TolledTrips
		SortedMap<Double,List<Id<Person>>> timeBin2TolledPersonsList = this.cda.getTimeBin2ListOfTollPayers();
		for(Double d : timeBin2TolledPersonsList.keySet()){
			SortedMap<UserGroup,Integer> usrGrp2Trips = this.userGroup2TolledTrips.get(d);
			
			for(Id<Person> personId : timeBin2TolledPersonsList.get(d)){
				UserGroup ug = this.pf.getUserGroupFromPersonId(personId);
				usrGrp2Trips.put(ug, usrGrp2Trips.get(ug)+1);
			}
		}

		//timeBin2TolledLinks
		SortedMap<Double, Map<Id<Link>, Double>> timeBin2LinkDelay = this.cda.getTimeBin2LinkId2Delay();
		for(Double d : timeBin2LinkDelay.keySet()){
			int cnt = 0;
			for (Id<Link> linkId : timeBin2LinkDelay.get(d).keySet()){
				if(timeBin2LinkDelay.get(d).get(linkId)!=0) cnt++;
			}
			this.timeBin2TolledLinks.put(d, cnt);
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/tollPayerCountInfo"+suffixForSoring+".txt");
		try {
			writer.write("TimeBin \t UserGroup \t totaltollPayers \t totalTolledTrips \n");
			for(Double d : this.userGroup2TollPayers.keySet()){
				for(UserGroup ug : this.userGroup2TollPayers.get(d).keySet()){
					writer.write(d+"\t"+ug+"\t"+this.userGroup2TollPayers.get(d).get(ug)+"\t"+this.userGroup2TolledTrips.get(d).get(ug)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}

		writer = IOUtils.getBufferedWriter(outputFolder+"/tolledLinkCountInfo"+suffixForSoring+".txt");
		try {
			writer.write("TimeBin \t totalTolledLinks \n");
			for(Double d : this.timeBin2TolledLinks.keySet()){
				writer.write(d+"\t"+this.timeBin2TolledLinks.get(d)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

}
