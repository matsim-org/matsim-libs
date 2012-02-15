package playground.ciarif.flexibletransports.preprocess.membership.strategies;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.LinkImpl;

import playground.ciarif.data.FacilitiesPortfolio;
import playground.ciarif.flexibletransports.preprocess.membership.MembershipAssigner;
import playground.ciarif.flexibletransports.preprocess.membership.MembershipModel;
import playground.ciarif.flexibletransports.preprocess.membership.algos.MembershipGA;
import playground.ciarif.retailers.RetailerGA.RunRetailerGA;
import playground.ciarif.retailers.models.MinTravelCostsModel;

public class CSmembershipMaximizationStrategy extends CSstationsLocationStrategy {
	
	private MembershipModel membershipModel;
	private MembershipAssigner membershipAssigner;
	private Scenario scenario;
	
	
	
	CSmembershipMaximizationStrategy () {
		
	}
	
	@Override
	public void findOptimalLocations(FacilitiesPortfolio facilitiesPortfolio,
			TreeMap<Id, LinkImpl> links) {
		
//		MinTravelCostsModel mam = new MinTravelCostsModel(this.controler, retailerFacilities);
//	    TreeMap first = createInitialLocationsForGA(mergeLinks(freeLinks, retailerFacilities));
//	    log.info("first = " + first);
//	    mam.init(first);
//	    this.shops = mam.getScenarioShops();
//	    Integer populationSize = Integer.valueOf(Integer.parseInt(this.controler.getConfig().findParam("Retailers", "PopulationSize")));
//	    if (populationSize == null) log.warn("In config file, param = PopulationSize in module = Retailers not defined, the value '10' will be used as default for this parameter");
//	    Integer numberGenerations = Integer.valueOf(Integer.parseInt(this.controler.getConfig().findParam("Retailers", "numberOfGenerations")));
//	    if (numberGenerations == null) log.warn("In config file, param = numberOfGenerations in module = Retailers not defined, the value '100' will be used as default for this parameter");
//	    RunRetailerGA rrGA = new RunRetailerGA(populationSize, numberGenerations);
//	    ArrayList<Integer> solution = rrGA.runGA(mam);
		
	}

}
