package playground.jhackney.replanning;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Knowledge;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.socialnetworks.socialnet.EgoNet;

public class SNPickFacility implements PlanAlgorithm {


	private final String weights;
	private double[] cum_p_factype;
	private NetworkLayer network;
	private TravelCost tcost;
	private TravelTime ttime;
	private String[] factypes;

	public SNPickFacility(String[] factypes, NetworkLayer network, TravelCost tcost, TravelTime ttime){
		weights = Gbl.getConfig().socnetmodule().getSWeights();
		cum_p_factype = getCumFacWeights(weights);
		this.network=network;
		this.tcost=tcost;
		this.ttime=ttime;
		this.factypes=factypes;
	}

	public void run(Plan plan) {
		// TODO Auto-generated method stub
		pickFacility(plan);
	}

	private void pickFacility(Plan plan) {
		// TODO Auto-generated method stub
		String factype=null;// facility type to switch out
		Person person = plan.getPerson();

		//COPY THE SELECTED PLAN		    
		Plan newPlan = person.copySelectedPlan();

		// Note that it is not changed, yet
		boolean changed = false;

//		Pick a type of facility to replace in this plan according to config settings
		double rand = MatsimRandom.random.nextDouble();

		if (rand < cum_p_factype[0]) {
			factype = factypes[0];
		}else if (cum_p_factype[0] <= rand && rand < cum_p_factype[1]) {
			factype = factypes[1];
		}else if (cum_p_factype[1] <= rand && rand < cum_p_factype[2]) {
			factype = factypes[2];
		}else if (cum_p_factype[2] <= rand && rand < cum_p_factype[3]) {
			factype = factypes[3];
		}else {
			factype = factypes[4];
		}

//		Get all instances of this facility type in the plan

		ActIterator planIter= newPlan.getIteratorAct();
		ArrayList<Act> actsOfFacType= new ArrayList<Act>();
		while(planIter.hasNext()){
			Act nextAct=(Act) planIter.next();
			if(nextAct.getType()== factype){
				actsOfFacType.add(nextAct);
			}
		}

		// Choose a random act from this list. Return the plan unchanged if there are none.
		if(actsOfFacType.size()<1){
			person.setSelectedPlan(plan);
			person.getPlans().remove(newPlan);
			return;

		}else{

			Act newAct = (Act)(actsOfFacType.get(MatsimRandom.random.nextInt(actsOfFacType.size())));

//			Get agent's knowledge
			Knowledge k = person.getKnowledge();

			Hashtable<Activity,Integer> facMap=new Hashtable<Activity,Integer>();
			ArrayList<Activity> facList=k.getActivities(factype);
			Iterator<Activity> fIt=facList.iterator();
			while(fIt.hasNext()){
				Activity activity=fIt.next();
				if(!(facMap.containsKey(activity))){
					facMap.put(activity,1);
				}else{
					int m=facMap.get(activity)+1;
					facMap.put(activity,m);
				}
			}
			
//			Pick a new facility for it from the knowledge of alters and ego (LOGIT)
//For each alter append its facList to existing facList
			EgoNet egoNet = k.getEgoNet();
			ArrayList<Person> alters=egoNet.getAlters();
			Iterator<Person> aIt=alters.iterator();
			while(aIt.hasNext()){
				Person alter = aIt.next();
				facList.clear();
				facList.addAll(alter.getKnowledge().getActivities(factype));
				fIt=null;
				fIt=facList.iterator();
				while(fIt.hasNext()){
					Activity activity=fIt.next();
					if(!(facMap.keySet().contains(activity))){
						facMap.put(activity,1);
					}else{
						int m=facMap.get(activity)+1;
						facMap.put(activity,m);
					}
				}
			}
			Facility f = getFacByLogit(facMap);
			
			
//----------
			
//			And replace the activity in the chain with it (only changes the facility)

			if(newAct.getLinkId()!=f.getLink().getId()){
				// If the first activity was chosen, make sure the last activity is also changed
				if(newAct.getType() == plan.getFirstActivity().getType() && newAct.getLink() == plan.getFirstActivity().getLink()){
					Act lastAct = (Act) newPlan.getActsLegs().get(newPlan.getActsLegs().size()-1);
					lastAct.setLink(f.getLink());
					lastAct.setLinkId(f.getLink().getId());
					lastAct.setCoord(f.getCenter());
					lastAct.setFacility(f);
				}
				// If the last activity was chosen, make sure the first activity is also changed
				if(newAct.getType() == ((Act)plan.getActsLegs().get(plan.getActsLegs().size()-1)).getType() && newAct.getLink() == ((Act)plan.getActsLegs().get(plan.getActsLegs().size()-1)).getLink()){
					Act firstAct = (Act) newPlan.getFirstActivity();
					firstAct.setLink(f.getLink());
					firstAct.setLinkId(f.getLink().getId());
					firstAct.setCoord(f.getCenter());
					firstAct.setFacility(f);
				}
				// Change the activity
//				System.out.println("  ##### Act at "+newAct.getFacility().getId()+" of type "+newAct.getType()+" ID "+newAct.getLink().getId()+" was changed for person "+plan.getPerson().getId()+" to "+f.getLink().getId());
				newAct.setLink(f.getLink());
				newAct.setLinkId(f.getLink().getId());
				newAct.setCoord(f.getCenter());
				newAct.setFacility(f);
				changed = true;
			}

			if(changed){
				k.getMentalMap().addActivity(f.getActivity(factype));
				System.out.println(" Activity locatoin changed this many activities:"+k.getActivities().size());
				//		 loop over all <leg>s, remove route-information
				ArrayList<?> bestactslegs = newPlan.getActsLegs();
				for (int j = 1; j < bestactslegs.size(); j=j+2) {
					Leg leg = (Leg)bestactslegs.get(j);
					leg.setRoute(null);
				}
//				Reset the score.
				newPlan.setScore(Plan.UNDEF_SCORE);

				new PersonPrepareForSim(new PlansCalcRoute(network, tcost, ttime), network).run(newPlan.getPerson());
//				new PlansCalcRoute(network, tcost, ttime).run(newPlan);

//				Not needed with new change to Act --> Facility JH 7.2008
//				k.getMentalMap().learnActsActivities(newAct,f.getActivity(factype));
				person.setSelectedPlan(newPlan);
//				System.out.println("   ### new location for "+person.getId()+" "+newAct.getType());

			}else{
//				System.out.println("   ### newPlan same as old plan");
				person.getPlans().remove(newPlan);
				person.setSelectedPlan(plan);
			}
		}
	}

	private Facility getFacByLogit(Hashtable<Activity, Integer> facMap) {
		// TODO Auto-generated method stub
		Facility fac=null;
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
		double random = MatsimRandom.random.nextDouble()*p[p.length-1];
		int pick=0;
		for(int i=0;i<nums.length;i++){
			if(i==0){
				if(random<=p[i]) pick=0;
			}else if(p[i-1]<random && random<=p[i]){
				pick=i;
			}
		}
		fac=((Activity) facMap.keySet().toArray()[pick]).getFacility();
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
