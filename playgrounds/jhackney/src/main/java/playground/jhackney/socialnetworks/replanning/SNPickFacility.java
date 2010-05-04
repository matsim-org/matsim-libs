package playground.jhackney.socialnetworks.replanning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.socialnetworks.mentalmap.MentalMap;
import playground.jhackney.socialnetworks.socialnet.EgoNet;
/**
 * A replanning module to choose a new location for one activity in the Ego's SelectedPlan.
 * The new location is chosen from the combined Knowledge about Facilities of Ego and Alters.
 * The activity to re-locate is chosen randomly based on a user-defined
 * probability for changing the location of activities.<br><br>
 *
 * <li>1) The choice of location is made using a multi-nomial Logit implementation in which the
 * alternatives are weighted by the frequency with which the location appears
 * in the Ego and Alters' Knowledge.</li>
 * <li>2) If the new location is different from the old location, it is adopted by the Ego and added
 * to his Knowledge.</li><br><br>
 * Thus this module represents bounded rational decision making on a complex information network.
 * First, there is a search
 * through a limited alternative space (defined by Ego and Alters' Knowledge), followed by an exchange of information that is based on
 * the popularity of the information. If one combines this adoption of information with a system to
 * manage cognitive limits of agents, it forms an evolutionary algorithm for the spread of
 * information about facilities.<br><br>
 * If there are no Alters defined in the SocialNetwork, the Ego decides on a new location
 * based on his own Knowledge, and the module reduces to
 * {@link playground.jhackney.socialnetworks.replanning.RandomFacilitySwitcherK}.
 * The difference between the two modules is that this one REQUIRES a social network
 * to be defined (it can be empty) and the other does not require a social network.<br><br>
 *
 * Required objects:<br>
 * <li>Facilities: as a "location choice" strategy, the module works explicitly with facilities</li>
 * <li>SocialNetwork: {@link playground.jhackney.socialnetworks.socialnet.SocialNetwork} must be non-null</li>
 * <li>weights: string of weights from [0,1] indicating the probability of
 * switching location of each type of facility that exists in the model.<br><br>
 *
 * @author jhackney
 *
 */
public class SNPickFacility implements PlanAlgorithm {


	private final String weights;
	private final double[] cum_p_factype;
	private final Network network;
	private final PersonalizableTravelCost tcost;
	private final TravelTime ttime;
	private final String[] factypes;
	private final Logger log = Logger.getLogger(SNPickFacility.class);
	private final Knowledges knowledges;

	public SNPickFacility(String[] factypes, NetworkLayer network, PersonalizableTravelCost tcost, TravelTime ttime, Knowledges knowledges, SocNetConfigGroup snConfig){
		weights = snConfig.getSWeights();
		cum_p_factype = getCumFacWeights(weights);
		this.network=network;
		this.tcost=tcost;
		this.ttime=ttime;
		this.factypes=factypes;
		this.knowledges = knowledges;
	}

	public void run(Plan plan) {
		pickFacility(plan);
	}

	private void pickFacility(Plan plan) {
		String factype=null;// facility type to switch out
		Person person = plan.getPerson();

		//COPY THE SELECTED PLAN
		Plan newPlan = ((PersonImpl) person).copySelectedPlan();

		// Note that it is not changed, yet
		boolean changed = false;

//		Pick a type of facility to replace in this plan according to config settings
		double rand = MatsimRandom.getRandom().nextDouble();

		if (rand < cum_p_factype[0]) {
			factype = factypes[0];
		}else if ((cum_p_factype[0] <= rand) && (rand < cum_p_factype[1])) {
			factype = factypes[1];
		}else if ((cum_p_factype[1] <= rand) && (rand < cum_p_factype[2])) {
			factype = factypes[2];
		}else if ((cum_p_factype[2] <= rand) && (rand < cum_p_factype[3])) {
			factype = factypes[3];
		}else {
			factype = factypes[4];
		}

//		Get all instances of this facility type in the plan

		ArrayList<ActivityImpl> actsOfFacType= new ArrayList<ActivityImpl>();
		for (PlanElement pe : newPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl nextAct=(ActivityImpl) pe;
				if(nextAct.getType().equals(factype)){
					actsOfFacType.add(nextAct);
				}
			}
		}

		// Choose a random act from this list. Return the plan unchanged if there are none.
		if(actsOfFacType.size()<1){
			((PersonImpl) person).setSelectedPlan(plan);
			person.getPlans().remove(newPlan);
			return;

		}else{

			ActivityImpl newAct = (actsOfFacType.get(MatsimRandom.getRandom().nextInt(actsOfFacType.size())));

//			Get agent's knowledge
			KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(person.getId());

			LinkedHashMap<ActivityOptionImpl,Integer> facMap=new LinkedHashMap<ActivityOptionImpl,Integer>();
			ArrayList<ActivityOptionImpl> facList=k.getActivities(factype);
			Iterator<ActivityOptionImpl> fIt=facList.iterator();
			while(fIt.hasNext()){
				ActivityOptionImpl activity=fIt.next();
				if(!(facMap.containsKey(activity))){
					facMap.put(activity,1);
				}else{
					int m=facMap.get(activity)+1;
					facMap.put(activity,m);
				}
			}

//			Pick a new facility for it from the knowledge of alters and ego (LOGIT)
//For each alter append its facList to existing facList
			EgoNet egoNet = (EgoNet)person.getCustomAttributes().get(EgoNet.NAME);
			ArrayList<Person> alters=egoNet.getAlters();
			Iterator<Person> aIt=alters.iterator();
			while(aIt.hasNext()){
				Person alter = aIt.next();
				facList.clear();
				facList.addAll(this.knowledges.getKnowledgesByPersonId().get(alter.getId()).getActivities(factype));
				fIt=null;
				fIt=facList.iterator();
				while(fIt.hasNext()){
					ActivityOptionImpl activity=fIt.next();
					if(!(facMap.keySet().contains(activity))){
						facMap.put(activity,1);
					}else{
						int m=facMap.get(activity)+1;
						facMap.put(activity,m);
					}
				}
			}
			ActivityFacilityImpl f = getFacByLogit(facMap);


//----------

//			And replace the activity in the chain with it (only changes the facility)

			if(!newAct.getLinkId().equals(f.getLinkId())){
				// If the first activity was chosen, make sure the last activity is also changed
				if((newAct.getType() == ((PlanImpl) plan).getFirstActivity().getType()) && (newAct.getLinkId().equals(((PlanImpl) plan).getFirstActivity().getLinkId()))){
					ActivityImpl lastAct = (ActivityImpl) newPlan.getPlanElements().get(newPlan.getPlanElements().size()-1);
					lastAct.setLinkId(f.getLinkId());
					lastAct.setCoord(f.getCoord());
					lastAct.setFacilityId(f.getId());
				}
				// If the last activity was chosen, make sure the first activity is also changed
				if((newAct.getType() == ((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType()) && (newAct.getLinkId().equals(((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getLinkId()))){
					ActivityImpl firstAct = (ActivityImpl) ((PlanImpl) newPlan).getFirstActivity();
					firstAct.setLinkId(f.getLinkId());
					firstAct.setCoord(f.getCoord());
					firstAct.setFacilityId(f.getId());
				}
				// Change the activity
//				System.out.println("  ##### Act at "+newAct.getFacility().getId()+" of type "+newAct.getType()+" ID "+newAct.getLink().getId()+" was changed for person "+plan.getPerson().getId()+" to "+f.getLink().getId());
				newAct.setLinkId(f.getLinkId());
				newAct.setCoord(f.getCoord());
				newAct.setFacilityId(f.getId());
				changed = true;
			}

			if(changed){
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).addActivity(f.getActivityOptions().get(factype));
//				System.out.println(" Activity locatoin changed this many activities:"+k.getActivities().size());
				//		 loop over all <leg>s, remove route-information
				List<? extends PlanElement> bestactslegs = newPlan.getPlanElements();
				for (int j = 1; j < bestactslegs.size(); j=j+2) {
					LegImpl leg = (LegImpl)bestactslegs.get(j);
					leg.setRoute(null);
				}
//				Reset the score.
//				newPlan.setScore(null);

				new PersonPrepareForSim(new PlansCalcRoute(null, network, tcost, ttime, new DijkstraFactory()), (NetworkLayer) network).run(newPlan.getPerson());
//				new PlansCalcRoute(network, tcost, ttime).run(newPlan);

//				Not needed with new change to Act --> Facility JH 7.2008
//				k.getMentalMap().learnActsActivities(newAct,f.getActivity(factype));
				((PersonImpl) person).setSelectedPlan(newPlan);
//				System.out.println("   ### new location for "+person.getId()+" "+newAct.getType());

			}else{
//				System.out.println("   ### newPlan same as old plan");
				person.getPlans().remove(newPlan);
				((PersonImpl) person).setSelectedPlan(plan);
			}
		}
	}

	private ActivityFacilityImpl getFacByLogit(LinkedHashMap<ActivityOptionImpl, Integer> facMap) {
		ActivityFacilityImpl fac=null;
		Object[] nums=facMap.values().toArray();
		Double[] p= new Double[nums.length];
		for(int i=0;i<nums.length;i++){
			if(i==0){
				p[i]=Math.exp(Double.parseDouble(nums[i].toString()));
			}
			else{
//				System.out.println(i+" "+nums.length);
//				System.out.println(p[i-1]+" "+nums[i]);
//				System.out.println(Math.exp(Double.parseDouble(nums[i].toString())));
				p[i]=p[i-1]+Math.exp(Double.parseDouble(nums[i].toString()));
			}
		}
		double random = MatsimRandom.getRandom().nextDouble()*p[p.length-1];
		int pick=0;
		for(int i=0;i<nums.length;i++){
			if(i==0){
				if(random<=p[i]) pick=0;
			}else if((p[i-1]<random) && (random<=p[i])){
				pick=i;
			}
		}
		fac=((ActivityOptionImpl) facMap.keySet().toArray()[pick]).getFacility();
		return fac;
	}

	private double[] getCumFacWeights(String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		w[0]=Double.parseDouble(s[0]);
		double sum = w[0];
		for (int i = 1; i < s.length; i++) {
			w[i] = Double.parseDouble(s[i])+w[i-1];
			sum=sum+Double.parseDouble(s[i]);
		}
		if (sum > 0) {
			for (int i = 0; i < s.length; i++) {

				w[i] = w[i] / sum;
			}
		} else if (sum < 0) {
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}
}
