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
package playground.agarwalamit.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;

/**
 * @author amit
 */

public class LinkTollFromExternalCosts {

	private final int noOfTimeBin;
	private final boolean considerCO2Costs ;

	public LinkTollFromExternalCosts (){
		this(1,true);
	}

	public LinkTollFromExternalCosts (int noOfTimeBin){
		this(noOfTimeBin,true);
	}

	public LinkTollFromExternalCosts (int noOfTimeBin, boolean considerCO2Costs){
		this.noOfTimeBin = noOfTimeBin;
		this.considerCO2Costs = considerCO2Costs;
	}

	/**
	 * @param internalizeEmissionOrCongestion emission congestion or both
	 * Total external costs includes only emission and caused congestion costs. This is basically toll on each link 
	 */
	public Map<Id<Link>, Double> getLinkToTotalExternalCost(final Scenario sc, final String internalizeEmissionOrCongestion){

		if(noOfTimeBin!=1) throw new RuntimeException("This method is not yet adapted to more than 1 time bin. Aborting ....");
		double simEndTime = 30*2400;

		if(internalizeEmissionOrCongestion.equalsIgnoreCase("ei")) return getLink2EmissionToll(sc);
		else if(internalizeEmissionOrCongestion.equalsIgnoreCase("ci")) return getLink2CongestionToll(sc).get(simEndTime);
		else if(internalizeEmissionOrCongestion.equalsIgnoreCase("eci")) {

			Map<Id<Link>, Double> link2delaycost =  getLink2CongestionToll(sc).get(simEndTime);
			Map<Id<Link>, Double> link2emissioncost = getLink2EmissionToll(sc);

			Map<Id<Link>, Double> totalCost = new HashMap<>();

			for(Link l : sc.getNetwork().getLinks().values()){
				Id<Link> linkId = l.getId();

				if(link2delaycost.containsKey(linkId) && link2emissioncost.containsKey(linkId)){
					totalCost.put(linkId, link2delaycost.get(linkId)+link2emissioncost.get(linkId));
				}else if(link2emissioncost.containsKey(linkId)){
					totalCost.put(linkId, link2emissioncost.get(linkId));
				}else totalCost.put(linkId, link2delaycost.get(linkId));

			}
			return totalCost;
		} else throw new RuntimeException("Do not recognize the external costs option. Available options are ei, ci or eci. Aborting ...");
	}

	/**
	 * @param sc 
	 * @return link to caused congestion costs which is basically equal to the toll.
	 */
	public Map<Double,Map<Id<Link>, Double>> getLink2CongestionToll(final Scenario sc){

		Map<Double, Map<Id<Link>, Double>>  linkTolls = new HashMap<>();

		Config config = sc.getConfig();

		double vttsCar = ((config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() /3600) +
				(config.planCalcScore().getPerforming_utils_hr()/3600)) 
				/ (config.planCalcScore().getMarginalUtilityOfMoney());

		int lastIt = config.controler().getLastIteration();
		String eventsFile = config.controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";

		CausedDelayAnalyzer delayAnalyzer = new CausedDelayAnalyzer(eventsFile,sc,noOfTimeBin);
		delayAnalyzer.run();
		Map<Double, Map<Id<Link>, Double>> linkDelays = delayAnalyzer.getTimeBin2LinkId2Delay();

		for (double d :  linkDelays.keySet()){
			Map<Id<Link>,Double > tolls = new HashMap<>();
			for (Id<Link> linkid : linkDelays.get(d).keySet()){
				tolls.put(linkid, linkDelays.get(d).get(linkid) * vttsCar);
			}
			linkTolls.put(d,tolls);
		}
		return linkTolls;
	}

	public Map<Id<Link>, Double> getLink2EmissionToll(final Scenario sc){
		int lastIt = sc.getConfig().controler().getLastIteration();
		String emissionEventsFile = sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".emission.events.xml.gz";
		EmissionLinkAnalyzer emissionAnalyzer = new EmissionLinkAnalyzer(sc.getConfig().qsim().getEndTime(), emissionEventsFile, noOfTimeBin);
		emissionAnalyzer.preProcessData();
		emissionAnalyzer.postProcessData();
		SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> link2TotalEmissions = emissionAnalyzer.getLink2TotalEmissions();

		Map<Id<Link>, Double> linkEmissionCosts = new HashMap<>();

		if(noOfTimeBin!=1) throw new RuntimeException("This method is not yet adapted to more than 1 time bin. Aborting ....");

		for(double d : link2TotalEmissions.keySet()){
			for(Id<Link> link : sc.getNetwork().getLinks().keySet()){
				double emissionsCosts = 0;
				for(EmissionCostFactors ecf:EmissionCostFactors.values()) {
					String str = ecf.toString();

					if(!str.equals("CO2_TOTAL") || considerCO2Costs){
						if(link2TotalEmissions.get(d).get(link)!=null && link2TotalEmissions.get(d).get(link).get(str) !=null) 
							emissionsCosts += ecf.getCostFactor() * link2TotalEmissions.get(d).get(link).get(str);
					} //else do nothing

				}
				linkEmissionCosts.put(link, emissionsCosts);
			}
		}
		return linkEmissionCosts;
	}
}