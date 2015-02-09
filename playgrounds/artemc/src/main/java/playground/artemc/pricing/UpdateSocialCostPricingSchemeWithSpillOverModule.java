package playground.artemc.pricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import javax.inject.Inject;
import java.util.ArrayList;


/**
 * Created by artemc on 2/2/15.
 */
public class UpdateSocialCostPricingSchemeWithSpillOverModule extends AbstractModule {

	static private Logger log;

	public UpdateSocialCostPricingSchemeWithSpillOverModule(){
		this.log = Logger.getLogger(UpdateSocialCostPricingSchemeWithSpillOverModule.class);
	}

	@Override
	public void install() {
		System.out.println();
		addControlerListener(costUpdater.class);
	}

	private static class costUpdater implements IterationStartsListener,IterationEndsListener, ControlerListener {

		RoadPricingSchemeImpl roadPricingScheme;
		SocialCostCalculator scc;
		LinkOccupancyAnalyzer linkOccupancyAnalyzer;

		private int timeslice;
		private final double blendFactor = 0.1;

		@Inject
		public costUpdater(RoadPricingScheme roadPricingScheme, LinkOccupancyAnalyzer linkOccupancyAnalyzer) {
			this.roadPricingScheme = (RoadPricingSchemeImpl) roadPricingScheme;
			this.linkOccupancyAnalyzer = linkOccupancyAnalyzer;
		}

//		//setter method injector
//		@Inject
//		public void setService(RoadPricingScheme roadPricingScheme){
//			this.roadPricingScheme = (RoadPricingSchemeImpl) roadPricingScheme;
//		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			if(event.getIteration()==0) {
				Controler controler = event.getControler();
				this.timeslice = controler.getConfig().travelTimeCalculator().getTraveltimeBinSize();
				this.scc = new SocialCostCalculator(controler.getScenario().getNetwork(), timeslice, controler.getEvents(), controler.getLinkTravelTimes(), controler, blendFactor);
				controler.addControlerListener(scc);
				controler.getEvents().addHandler(scc);
			}
		}

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {

			Controler controler = event.getControler();

			log.info("Updating tolls according to social cost imposed...");

			// initialize the social costs calculator

			for (Id<Link> link : event.getControler().getScenario().getNetwork().getLinks().keySet()) {

				Link networkLink = controler.getScenario().getNetwork().getLinks().get(link);

			if(roadPricingScheme.getTypicalCostsForLink().containsKey(link))
				roadPricingScheme.getTypicalCostsForLink().get(link).clear();


					for (int i = 0; i < scc.getSocialCostsMap().get(link).socialCosts.length; i++) {
						double socialCost = scc.getSocialCostsMap().get(link).socialCosts[i];
						double opportunityCostOfCarTravel = - controler.getConfig().planCalcScore().getTraveling_utils_hr() + controler.getConfig().planCalcScore().getPerforming_utils_hr();
						double toll = (opportunityCostOfCarTravel * socialCost / 3600) / controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();

						if(toll<0.01){
							toll=0.0;
							scc.getSocialCostsMap().get(link).socialCosts[i] = 0.0;
						}else{
							for(Id<Link> outLinkId:networkLink.getToNode().getOutLinks().keySet()){
								Link outLink = controler.getScenario().getNetwork().getLinks().get(outLinkId);
								double maxCapacity = outLink.getNumberOfLanes()*outLink.getLength()*controler.getConfig().qsim().getStorageCapFactor()/7.5;
								if(linkOccupancyAnalyzer.getOccupancyPercentage(outLinkId,(double) i* (double) this.timeslice, 0.99*maxCapacity)>0.2){
									//if(linkOccupancyAnalyzer.getAverageLinkOccupancy(outLinkId,(double) i* (double) this.timeslice)>0.8*maxCapacity){
									log.info("Toll for link "+link.toString()+" is set to zero, as outgoing link "+outLinkId+" is full.");
									toll = 0.0;
								}
							}
						}

						RoadPricingSchemeImpl.Cost cost = new RoadPricingSchemeImpl.Cost(i * timeslice, (i + 1) * timeslice - 1, toll);

						if(!roadPricingScheme.getTypicalCostsForLink().containsKey(link)){
							roadPricingScheme.getTypicalCostsForLink().put(link, new ArrayList<RoadPricingSchemeImpl.Cost>());
						}
						roadPricingScheme.getTypicalCostsForLink().get(link).add(cost);
					}
				}

		}
	}


}
