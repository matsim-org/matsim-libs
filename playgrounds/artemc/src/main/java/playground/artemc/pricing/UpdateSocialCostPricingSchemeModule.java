package playground.artemc.pricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
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
public class UpdateSocialCostPricingSchemeModule extends AbstractModule {

	static private Logger log;

	public UpdateSocialCostPricingSchemeModule(){
		this.log = Logger.getLogger(UpdateSocialCostPricingSchemeModule.class);
	}

	@Override
	public void install() {
		System.out.println();
		addControlerListenerBinding().to(costUpdater.class);
	}

	private static class costUpdater implements IterationStartsListener,IterationEndsListener, ControlerListener {

		RoadPricingSchemeImpl roadPricingScheme;
		SocialCostCalculator scc;

		/*Time bin size for calculation of social cost and toll setting. Should be the same as for the router.*/
		private final int timeslice = 5 * 60;

		/*Smoothing factor of toll changes: New toll = OldToll * (1-blendFactor) + NewToll * blendFactor
		* blendFactor = 1.0 leads to no smoothing at all, but given a small network and high demand can lead to unstable behaviour.
		  blendFactor = 0.1 seems to be a good compromise between convergence speed and stability.  */
		private final double blendFactor = 0.1;

		@Inject
		public costUpdater(RoadPricingScheme roadPricingScheme) {
			this.roadPricingScheme = (RoadPricingSchemeImpl) roadPricingScheme;
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

			if(roadPricingScheme.getTypicalCostsForLink().containsKey(link))
				roadPricingScheme.getTypicalCostsForLink().get(link).clear();

					for (int i = 0; i < scc.getSocialCostsMap().get(link).socialCosts.length; i++) {
						double socialCost = scc.getSocialCostsMap().get(link).socialCosts[i];
						double opportunityCostOfCarTravel = -controler.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() + controler.getConfig().planCalcScore().getPerforming_utils_hr();
						double toll = (opportunityCostOfCarTravel * socialCost / 3600) / controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();

						if(toll<0.01){
							toll=0.0;
							scc.getSocialCostsMap().get(link).socialCosts[i] = 0.0;
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
