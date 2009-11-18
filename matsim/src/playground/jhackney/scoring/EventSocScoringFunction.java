package playground.jhackney.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 *  SocializingScoringFunction2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.SocNetConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.ScoringFunction;


/**
 * A special {@linkplain ScoringFunction scoring function} that takes the face to face encounters
 * between the agents into account when calculating the score of a plan.
 *
 * @author jhackney
 */
public class EventSocScoringFunction extends playground.jhackney.scoring.CharyparNagelReportingScoringFunction {

	static final private Logger log = Logger.getLogger(EventSocScoringFunction.class);
//	private final playground.jhackney.scoring.CharyparNagelScoringFunctiosuperon;
	private final Plan plan;
//	private final TrackEventsOverlap teo;
	private final LinkedHashMap<Activity,ArrayList<Double>> actStats;
	private final String factype;

	private double friendFoeRatio=0.;
	private double nFriends=0;
	private double timeWithFriends=0;

	private SocNetConfigGroup socnetConfig = Gbl.getConfig().socnetmodule();

	private double betaFriendFoe = Double.parseDouble(socnetConfig.getBeta1());
	private double betaNFriends= Double.parseDouble(socnetConfig.getBeta2());
	private double betaLogNFriends= Double.parseDouble(socnetConfig.getBeta3());
	private double betaTimeWithFriends= Double.parseDouble(socnetConfig.getBeta4());
	LinkedHashMap<ActivityImpl,Double> usoc=new LinkedHashMap<ActivityImpl,Double>();
	LinkedHashMap<ActivityImpl,Double> dusoc=new LinkedHashMap<ActivityImpl,Double>();

//	public SocScoringFunctionEvent(final Plan plan, final playground.jhackney.scoring.CharyparNagelScoringFunction scoringFunction, String factype, final LinkedHashMap<Act,ArrayList<Double>> actStats) {
	public EventSocScoringFunction(final Plan plan, String factype, final LinkedHashMap<Activity,ArrayList<Double>> actStats) {
//		this.paidToll = paidToll;
		super(plan);
		this.plan = plan;
//		this.teo=teo;
		this.factype=factype;
		this.actStats=actStats;
		if(this.betaNFriends!= 0 && this.betaLogNFriends!=0){
			log.warn("Utility function values linear AND log number of Friends in spatial meeting");
		}
	}
	public double getUsoc(ActivityImpl a){
//		if(usoc.size()>0&&usoc.contains(a)){
			return usoc.get(a);
//			}else{
//				return 0;
//			}
	}
	@Override
	public double getUdur(ActivityImpl a){
		return super.getUdur(a);
	}
	@Override
	public double getUw(ActivityImpl a){
		return super.getUw(a);
	}
	@Override
	public double getUs(ActivityImpl a){
		return super.getUs(a);
	}
	@Override
	public double getUla(ActivityImpl a){
		return super.getUla(a);
	}
	@Override
	public double getUed(ActivityImpl a){
		return super.getUed(a);
	}
	@Override
	public double getUld(ActivityImpl a){
		return super.getUld(a);
	}
	@Override
	public double getUlegt(LegImpl l){
		return super.getUlegt(l);
	}
	@Override
	public double getUlegd(LegImpl l){
		return super.getUlegd(l);
	}

	public double getDusoc(ActivityImpl a){
		return dusoc.get(a);
	}
	@Override
	public double getDudur(ActivityImpl a){
		return super.getDudur(a);
	}
	@Override
	public double getDuw(ActivityImpl a){
		return super.getDuw(a);
	}
	@Override
	public double getDus(ActivityImpl a){
		return super.getDus(a);
	}
	@Override
	public double getDula(ActivityImpl a){
		return super.getDula(a);
	}
	@Override
	public double getDued(ActivityImpl a){
		return super.getDued(a);
	}
	@Override
	public double getDuld(ActivityImpl a){
		return super.getDuld(a);
	}
	@Override
	public double getDulegt(LegImpl l){
		return super.getDulegt(l);
	}
	@Override
	public double getDulegd(LegImpl l){
		return super.getDulegd(l);
	}
	
	/**
	 * Totals the act scores, including socializing during acts, for the entire plan
	 *
	 * @see org.matsim.scoring.super#finish()
	 */
	@Override
	public void finish() {
		super.finish();
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				double temp=0;
				double dtemp=0;
				if(act.getType().equals(factype)){
//				this.friendFoeRatio+=actStats.get(act).get(0);
//				this.nFriends+=actStats.get(act).get(1);
//				this.timeWithFriends+=actStats.get(act).get(2);
//				
					temp=betaFriendFoe*actStats.get(act).get(0)+
					betaNFriends *actStats.get(act).get(1)+
					betaLogNFriends * Math.log(actStats.get(act).get(1)+1)+
					betaTimeWithFriends * Math.log(actStats.get(act).get(2)/3600.+1);
					
					dtemp=betaFriendFoe+
					betaNFriends+
					betaLogNFriends/(actStats.get(act).get(1)+1)+
					betaTimeWithFriends/(actStats.get(act).get(2)/3600.+1);
				}
				this.score+=temp;
				usoc.put(act,temp);
				dusoc.put(act,dtemp);
			}
		}
	}


	@Override
	public double getScore() {
		
//		usoc.add(betaFriendFoe*this.friendFoeRatio+
//				betaNFriends * this.nFriends +
//				betaLogNFriends * Math.log(this.nFriends+1) +
//				betaTimeWithFriends * Math.log(this.timeWithFriends/3600.+1));
//
//		return super.getScore() +
//		betaFriendFoe*this.friendFoeRatio+
//		betaNFriends * this.nFriends +
//		betaLogNFriends * Math.log(this.nFriends+1) +
//		betaTimeWithFriends * Math.log(this.timeWithFriends/3600.+1);
	return score;
	}
}

