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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.userBenefits.MyUserBenefitsAnalyzer;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * A class to get user benefits and user welfare_money for each user group.
 * @author amit
 */
public class UserBenefitsAndTotalWelfarePerUserGroup {

	public static final Logger LOG = Logger.getLogger(UserBenefitsAndTotalWelfarePerUserGroup.class);
	private final String outputDir;
	private Map<Id<Person>, Double> personId2UserWelfareUtils;
	private Map<Id<Person>, Double> personId2MonetarizedUserWelfare;
	private Map<Id<Person>, Double> personId2MonetaryPayments;
	private SortedMap<MunichUserGroup, Double>  userGrp2ExcludedToll;
	private Scenario scenario;
	private int lastIteration;
	private final boolean considerAllPersonsInSumOfTolls;
	private final MunichPersonFilter pf = new MunichPersonFilter();
	private final WelfareMeasure wm = WelfareMeasure.SELECTED;
	
	/**
	 * @param outputDir
	 * @param considerAllPersonsInSumOfTolls if set to false, tolls for person having negative score will be reported separately.
	 */
	public UserBenefitsAndTotalWelfarePerUserGroup(final String outputDir, final boolean considerAllPersonsInSumOfTolls) {
		this.outputDir = outputDir;
		this.considerAllPersonsInSumOfTolls = considerAllPersonsInSumOfTolls;
	}

	public static void main(String[] args) {
		String outputDir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/exposure/";/*"./output/run2/";*/
		String [] runCases = {"ExI"};
		new UserBenefitsAndTotalWelfarePerUserGroup(outputDir, false).runAndWrite(runCases);
	}

	private void init(final String runCase){
		personId2UserWelfareUtils = new HashMap<>();
		personId2MonetarizedUserWelfare= new HashMap<>();
		personId2MonetaryPayments = new HashMap<>();
		userGrp2ExcludedToll = new TreeMap<>();

		this.scenario = LoadMyScenarios.loadScenarioFromOutputDir(this.outputDir+runCase);
		this.lastIteration = this.scenario.getConfig().controler().getLastIteration();
	}

	public void runAndWrite(final String [] runCases){

		for(String runCase:runCases){
			init(runCase);
			storeUserBenefitsMaps(runCase);
			getPersonId2MonetaryPayment(runCase);

			SortedMap<MunichUserGroup, Double> userGroupToUserWelfareUtils = getParametersPerUserGroup(this.personId2UserWelfareUtils);
			SortedMap<MunichUserGroup, Double> userGroupToUserWelfareMoney = getParametersPerUserGroup(this.personId2MonetarizedUserWelfare);
			SortedMap<MunichUserGroup, Double> userGroupToTotalPayment = getTollsPerUserGroup(this.personId2MonetaryPayments);

			String outputFile = this.outputDir+runCase+"/analysis/userGrpWelfareAndTollPayments_"+this.wm+".txt";
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

			double sumUtils =0;
			double sumUtilsMoney =0;
			double sumToll =0;
			double excludeTollSum =0;

			try {
				writer.write("UserGroup \t userWelfareUtils \t userWelfareMoney \t tollPayments \t excludedTollIfAny \n");
				for(MunichUserGroup ug : userGroupToTotalPayment.keySet()){
					writer.write(ug+"\t"+userGroupToUserWelfareUtils.get(ug)+"\t"
							+userGroupToUserWelfareMoney.get(ug)+"\t"
							+userGroupToTotalPayment.get(ug)+"\t"
							+userGrp2ExcludedToll.get(ug)+"\n");
					sumUtils += userGroupToUserWelfareUtils.get(ug);
					sumUtilsMoney += userGroupToUserWelfareMoney.get(ug);
					sumToll += userGroupToTotalPayment.get(ug);
					excludeTollSum += userGrp2ExcludedToll.get(ug);
				}
				writer.write("total \t"+sumUtils+"\t"+sumUtilsMoney+"\t"+sumToll+"\t"+excludeTollSum+"\n");
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException("Data is not written into a File. Reason : "+e);
			}
			LOG.info("Finished Writing data to file "+outputFile);	
		}
	}

	private SortedMap<MunichUserGroup, Double> getParametersPerUserGroup(final Map<Id<Person>, Double> inputMap) { 
		SortedMap<MunichUserGroup, Double> outMap = new TreeMap<>();
		for(MunichUserGroup ug : MunichUserGroup.values()){
			outMap.put(ug, 0.0);
		}

		for(Id<Person> id:inputMap.keySet()){
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(id);
			double valueSoFar = outMap.get(ug);
			double value2add = inputMap.get(id) ;
			double newValue = value2add+valueSoFar;
			outMap.put(ug, newValue);
		}
		return outMap;
	}

	private SortedMap<MunichUserGroup, Double> getTollsPerUserGroup(final Map<Id<Person>, Double> inputMap) { 
		SortedMap<MunichUserGroup, Double> outMap = new TreeMap<>();
		for(MunichUserGroup ug : MunichUserGroup.values()){
			outMap.put(ug, 0.0);
			userGrp2ExcludedToll.put(ug, 0.0);
		}

		for(Id<Person> id:inputMap.keySet()){
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(id);
			double valueSoFar = outMap.get(ug);
			double value2add = inputMap.get(id) ;

			if(!this.considerAllPersonsInSumOfTolls && !isPersonIncluded(id)){
				double excludeValue = userGrp2ExcludedToll.get(ug);
				userGrp2ExcludedToll.put(ug, excludeValue+value2add);
				value2add = 0;
			}

			double newValue = value2add+valueSoFar;
			outMap.put(ug, newValue);
		}
		return outMap;
	}

	private void storeUserBenefitsMaps(final String runCase){
		LOG.info("User welfare will be calculated using welfare measure as "+ wm.toString());

		MyUserBenefitsAnalyzer userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
		userBenefitsAnalyzer.init((MutableScenario)scenario, this.wm, false);
		userBenefitsAnalyzer.preProcessData();
		userBenefitsAnalyzer.postProcessData();

		userBenefitsAnalyzer.writeResults(this.outputDir+runCase+"/analysis/");

		this.personId2UserWelfareUtils = userBenefitsAnalyzer.getPersonId2UserWelfareUtils();
		this.personId2MonetarizedUserWelfare = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
	}

	private void getPersonId2MonetaryPayment(final String runCase){
		MonetaryPaymentsAnalyzer paymentsAnalyzer = new MonetaryPaymentsAnalyzer();
		paymentsAnalyzer.init((MutableScenario)scenario);
		paymentsAnalyzer.preProcessData();

		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = paymentsAnalyzer.getEventHandler();

		for(EventHandler eh : handler){
			events.addHandler(eh);
		}

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(this.outputDir+runCase+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".events.xml.gz");

		paymentsAnalyzer.postProcessData();
		paymentsAnalyzer.writeResults(this.outputDir+runCase+"/analysis/");
		this.personId2MonetaryPayments = paymentsAnalyzer.getPersonId2amount();
	}

	private boolean isPersonIncluded(final Id<Person> personId){
		double score = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
        return score >= 0;
	}
}