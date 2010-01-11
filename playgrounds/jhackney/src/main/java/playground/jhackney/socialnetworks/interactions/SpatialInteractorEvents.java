package playground.jhackney.socialnetworks.interactions;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;

import playground.jhackney.socialnetworks.algorithms.CompareTimeWindows;
import playground.jhackney.socialnetworks.mentalmap.TimeWindow;
import playground.jhackney.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import playground.jhackney.socialnetworks.socialnet.EgoNet;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;

/**
 * Class to permit face-to-face agent interactions via Events after the Mobsim.
 * 
 * @author jhackney
 *
 */
public class SpatialInteractorEvents {
	SocialNetwork net;

	double pBecomeFriends = Double.parseDouble(Gbl.getConfig().socnetmodule().getPBefriend());// [0.0,1.0]

//	double pct_interacting = Double.parseDouble(Gbl.getConfig().socnetmodule().getFractSInteract());// [0.0,1.0]

	String interaction_type = Gbl.getConfig().socnetmodule().getSocNetInteractor2();

//	LinkedHashMap<Activity,ArrayList<Person>> activityMap;
//	LinkedHashMap<Act,ArrayList<Person>> actMap=new LinkedHashMap<Act,ArrayList<Person>>();
	LinkedHashMap<Id,ArrayList<TimeWindow>> timeWindowMap;
//	TrackEventsOverlap teo;
	MakeTimeWindowsFromEvents teo;
	private final Logger log = Logger.getLogger(SpatialInteractorEvents.class);

//	public SpatialInteractorEvents(SocialNetwork snet, TrackEventsOverlap teo) {
	public SpatialInteractorEvents(SocialNetwork snet, MakeTimeWindowsFromEvents teo) {
		this.net = snet;
		this.teo=teo;
		log.warn("Methods are only for Undirected social interactions");
	}

	public void interact(Population plans, LinkedHashMap<String, Double> rndEncounterProb, int iteration,
			LinkedHashMap<Id,ArrayList<TimeWindow>> twm) {

		this.log.info(" Looking through plans and tracking which Persons could interact "+iteration);

		if(!(total_spatial_fraction(rndEncounterProb.values().toArray())>0)){
			this.log.info(" No spatial interactions "+iteration);
			return;
		}
		
//		this.timeWindowMap=teo.getTimeWindowMap();
		this.timeWindowMap=twm;

		// Activity-(facility)-based interactions

		if (interaction_type.equals("random")) {
			encounterOnePersonRandomlyPerActivity(rndEncounterProb, iteration);
		}else if (interaction_type.equals("meetall")){
			makeSocialLinkToAll(rndEncounterProb,iteration);

//			Act-based interactions

		}else if(interaction_type.equals("timewindowrandom")){
			encounterOnePersonRandomlyFaceToFaceInTimeWindow(rndEncounterProb,iteration);
		}else if(interaction_type.equals("timewindowall")){
			encounterAllPersonsFaceToFaceInTimeWindow(rndEncounterProb,iteration);
		}else if(interaction_type.equals("MarchalNagelChain")){
			Gbl.errorMsg("Spatial interactions "+interaction_type+" Not supported in new Act search algorithm "+ this.getClass());
//			makeSocialLinkBetweenLastTwo(rndEncounterProb,iteration);
		} else {
			Gbl.errorMsg(" Spatial interaction type is \"" + interaction_type
					+ "\". Only \"random\", \"meetall\", \"timewindowrandom\", and \"timewindowall\" are supported at this time.");
		}
		this.log.info("...finished");
	}

	/**
	 * Based on Marchal and Nagel 2007, TRR 1935
	 * Person p1 meets and befriends the person who arrived just before him, if
	 * this person is still at the SocialAct.
	 * 
	 * Cycle through all the agents who were co-present with p1 at the SocialAct
	 * and find the agent whose arrival time is closest to and less than that of p1
	 * Subject to the likelihood of a meeting taking place in a given facility type
	 */
	private void makeSocialLinkBetweenLastTwo(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {


//		Person nextInQueue=null;
//		if(Gbl.random.nextDouble() <rndEncounterProbability.get(act1.activity.getType())){

//		Vector<Person> persons = event.getAttendeesInTimeWindow(p1,startTime,endTime);
//		int size = persons.size();
//		nextInQueue = event.getAttendeeInTimeWindow(p1,startTime,endTime,0);
//		for(int i=0; i<size;i++){
//		Person p2 = event.getAttendeeInTimeWindow(p1, startTime, endTime, i);
//		if(event.getArrivalTime(p2)<=event.getArrivalTime(p1)){
//		if(event.getArrivalTime(p2)<=event.getArrivalTime(nextInQueue)){
//		nextInQueue=p2;
//		}
//		}
//		}
//		if(p1.getKnowledge().getEgoNet().knows(nextInQueue)){
//		} else {
//		// If the two do not already know each other,
//		net.makeSocialContact(p1,nextInQueue,iteration,"new"+event.activity.getType());
//		}
//		}
	}
	/**
	 * Time-independent chance of spatial encounter during each activity:
	 * 
	 * Each person visiting a Facility to perform an Activity has a chance
	 * to meet every other person who was at that Facility doing the same thing.
	 * <br><br>
	 * This models the chance that two people who do the same thing at the same
	 * place, but who may not have been present in the same time window because
	 * of bad luck or bad planning, might still know each other.
	 * <br><br>
	 * For every ordered pair, person1 and person2, who visited the same facility
	 * and performed the same activity there, regardless of when, there is a probability
	 * that they will be linked: p1 <-> p2. 
	 * <br><br>
	 * To make this valid for directed networks, the loop over timeWindows would have
	 * to be nested so that there is a chance that p1->p2 and then a second
	 * chance that p2 -> p1.
	 * <br><br>
	 * This probability depends only on the activity type, "rndEncounterProbability(activity type)",
	 * and not on any additional "friendliness" parameter.
	 * <br><br>
	 * The conditions of adding network links apply:
	 * {@link org.matsim.socialnetworks.socialnet.SocialNetwork.makeSocialContact}
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void makeSocialLinkToAll(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {
		for (Id facId : timeWindowMap.keySet()) {
			Object[] visits= timeWindowMap.get(facId).toArray();
			// Go through the list of Persons and for each one pick one friend randomly
			for(int ii=0;ii<visits.length;ii++){
				PersonImpl p1 = ((TimeWindow) visits[ii]).person;
				String actType1=((TimeWindow) visits[ii]).act.getType();
				for(int iii=ii;iii<visits.length;iii++){
					PersonImpl p2 = ((TimeWindow) visits[iii]).person;
					String actType2=((TimeWindow) visits[iii]).act.getType();
					if(actType1.equals(actType2)){
						if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(actType2)){

							// If they know each other, probability is 1.0 that the relationship is reinforced
							if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
								net.makeSocialContact(p1,p2,iteration,"renew_"+actType2);
//								System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
							} else {
								// If the two do not already know each other,
								net.makeSocialContact(p1,p2,iteration,"new_"+actType2);
//								System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Time-independent chance of spatial encounter at each activity.
	 * 
	 * Each person visiting a Facility to perform an Activity has a chance
	 * to meet ONE other person who was at that Facility doing the same thing.
	 * <br><br>
	 * For every ordered pair, person1 and person2, who visited the same facility
	 * and performed the same activity there, regardless of when, there is a probability
	 * that they will be linked: p1 <-> p2. 
	 * <br><br>
	 * To make this valid for directed networks, the loop over timeWindows would have
	 * to be nested so that there is a chance that p1->p2 and then a second
	 * chance that p2 -> p1.
	 * <br><br>
	 * If person1 and person2 visited the same facility and performed the same
	 * activity there, regardless of when, then there is a probability
	 * "rndEncounterProbability(activity type)" that they encounter one another.
	 * If they know each other, their friendship is reinforced.
	 * <br><br>
	 * If they do not, they befriend with probability <code>pBecomeFriends</code>.
	 * 
	 * The conditions of "becoming friends" apply:
	 * {@link org.matsim.socialnetworks.socialnet.SocialNetwork.makeSocialContact}
	 * 
	 * 
	 * @param p1
	 * @param p2
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void encounterOnePersonRandomlyPerActivity(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {
		for (Id facId : timeWindowMap.keySet()) {
			Object[] visits= timeWindowMap.get(facId).toArray();
			// Go through the list of Persons and for each one pick one friend randomly
			for(int ii=0;ii<visits.length;ii++){
				PersonImpl p1 = ((TimeWindow) visits[ii]).person;
				String actType1=((TimeWindow) visits[ii]).act.getType();

				TimeWindow tw2 = (TimeWindow) visits[MatsimRandom.getRandom().nextInt(visits.length)];
				PersonImpl p2 = tw2.person;
				String actType2=tw2.act.getType();
				if(actType1.equals(actType2)){
					if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(actType2)){

						// If they know each other, probability is 1.0 that the relationship is reinforced
						if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
							net.makeSocialContact(p1,p2,iteration,"renew_"+actType2);
//							System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
						} else {
							// If the two do not already know each other,

							if(MatsimRandom.getRandom().nextDouble() < pBecomeFriends){
								net.makeSocialContact(p1,p2,iteration,"new_"+actType2);
//								System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * Each agent may randomly encounter (and have the chance to befriend) ONE other agent during
	 * an Act in which they are both present. Uses a time window. The duration of the time
	 * window is not relevant in this method.
	 * <li>Construct a list of TimeWindows that overlap at a Facility, for each activity</li>
	 * <li>If this list is empty, continue</li>
	 * <li>If not, search this list for a random agent</li>
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void encounterOnePersonRandomlyFaceToFaceInTimeWindow(LinkedHashMap<String, Double> rndEncounterProbability, int iteration) {

		// First identify the overlapping Acts and the Persons involved
		for (Id facId : timeWindowMap.keySet()) {
			Object[] visits= timeWindowMap.get(facId).toArray();
			for(int ii=0;ii<visits.length;ii++){
				LinkedHashMap<String,ArrayList<PersonImpl>> othersMap = new LinkedHashMap<String,ArrayList<PersonImpl>>();
				TimeWindow tw1 = (TimeWindow) visits[ii];
				PersonImpl p1 = tw1.person;
				for (int iii=ii;iii<visits.length;iii++){
					TimeWindow tw2 = (TimeWindow) visits[iii];
					PersonImpl p2 = tw2.person;
					String actType2=tw2.act.getType();
					if(CompareTimeWindows.overlapTimePlaceType(tw1, tw2)){
						//agents encounter and may befriend
						//note that p2 could be present twice;
						//no problems here, but if we are counting duration
						//of time overlap, we need to account for that
						if(othersMap.containsKey(actType2)){
							ArrayList<PersonImpl> list=othersMap.get(actType2);
							list.add(p2);
							othersMap.remove(actType2);
							othersMap.put(actType2,list);
						}else{
							ArrayList<PersonImpl> list=new ArrayList<PersonImpl>();
							list.add(p2);
							othersMap.put(actType2, list);
						}
					}
				}
//				Enumerate the keys of others
				Object[] actTypes=othersMap.keySet().toArray();
				for (int j=0;j<actTypes.length;j++){
					ArrayList<PersonImpl> others = othersMap.get(actTypes[j]);

					if(others.size()>0){
						PersonImpl p2=others.get(MatsimRandom.getRandom().nextInt(others.size()));
						if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(actTypes[j])){

							// If they know each other, probability is 1.0 that the relationship is reinforced
							if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
								net.makeSocialContact(p1,p2,iteration,"renew_"+actTypes[j]);
//								System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
							} else {
								// If the two do not already know each other,

								if(MatsimRandom.getRandom().nextDouble() < pBecomeFriends){
									net.makeSocialContact(p1,p2,iteration,"new_"+actTypes[j]);
//									System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());
								}
							}
						}
					}
				}
			}
		}
	}




	/**
	 * For each Act of each SelectedPlan() of each Person, this method tests all other Acts
	 * at the Activity to see if they are in the same place at the same time (Act overlap). If so,
	 * the agents are linked in a social network with Activity-dependent probability,
	 * <code>rndEncounterProbability(activity type)</code>. If the agents were already linked,
	 * their link is reinforced.
	 * <br><br>
	 * 
	 * There is no other probability adjustment in this method.
	 * 
	 * @param plans
	 * @param rndEncounterProbability
	 * @param iteration
	 */
	private void encounterAllPersonsFaceToFaceInTimeWindow(LinkedHashMap<String, Double> rndEncounterProbability,
			int iteration) {

		for (Id facId : timeWindowMap.keySet()) {
			Object[] visits= timeWindowMap.get(facId).toArray();
			// Go through the list of Persons and for each one pick one friend randomly
			for(int ii=0;ii<visits.length;ii++){
				TimeWindow tw1 = (TimeWindow) visits[ii];
				PersonImpl p1 = tw1.person;
				for(int iii=ii;iii<visits.length;iii++){
					TimeWindow tw2=(TimeWindow) visits[iii];
					PersonImpl p2 = tw2.person;
					String actType2=tw2.act.getType();
					if(CompareTimeWindows.overlapTimePlaceType(tw1,tw2)&& !p1.equals(p2)){
						//agents encoutner and may befriend
						if(MatsimRandom.getRandom().nextDouble() <rndEncounterProbability.get(actType2)){
							// If they know each other, probability is 1.0 that the relationship is reinforced
							if (((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).knows(p2)) {
								net.makeSocialContact(p1,p2,iteration,"renew_"+actType2);
//								System.out.println("Person "+p1.getId()+" renews with Person "+ p2.getId());
							} else {
								// If the two do not already know each other,

								net.makeSocialContact(p1,p2,iteration,"new_"+actType2);
//								System.out.println("Person "+p1.getId()+" and Person "+ p2.getId()+" meet at "+myActivity.getFacility().getId()+" for activity "+myActivity.getType());

							}
						}
					}
				}
			}
		}
	}
	private double total_spatial_fraction(Object[] fractionS2) {
//		See if we use spatial interaction at all: sum of these must > 0 or else no spatial
//		interactions take place
		double total_spatial_fraction=0;
		for (int jjj = 0; jjj < fractionS2.length; jjj++) {
			total_spatial_fraction = (double) total_spatial_fraction + Double.parseDouble(fractionS2[jjj].toString());
		}
		return total_spatial_fraction;
	}
}
