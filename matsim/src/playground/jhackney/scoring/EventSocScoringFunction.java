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
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;


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
	private final LinkedHashMap<Act,ArrayList<Double>> actStats;
	private final String factype;

	private double friendFoeRatio=0.;
	private double nFriends=0;
	private double timeWithFriends=0;

	private SocNetConfigGroup socnetConfig = Gbl.getConfig().socnetmodule();

	private double betaFriendFoe = Double.parseDouble(socnetConfig.getBeta1());
	private double betaNFriends= Double.parseDouble(socnetConfig.getBeta2());
	private double betaLogNFriends= Double.parseDouble(socnetConfig.getBeta3());
	private double betaTimeWithFriends= Double.parseDouble(socnetConfig.getBeta4());
	LinkedHashMap<Act,Double> usoc=new LinkedHashMap<Act,Double>();
	LinkedHashMap<Act,Double> dusoc=new LinkedHashMap<Act,Double>();

//	public SocScoringFunctionEvent(final Plan plan, final playground.jhackney.scoring.CharyparNagelScoringFunction scoringFunction, String factype, final LinkedHashMap<Act,ArrayList<Double>> actStats) {
	public EventSocScoringFunction(final Plan plan, String factype, final LinkedHashMap<Act,ArrayList<Double>> actStats) {
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
	public double getUsoc(Act a){
//		if(usoc.size()>0&&usoc.contains(a)){
			return usoc.get(a);
//			}else{
//				return 0;
//			}
	}
	public double getUdur(Act a){
		return super.getUdur(a);
	}
	public double getUw(Act a){
		return super.getUw(a);
	}
	public double getUs(Act a){
		return super.getUs(a);
	}
	public double getUla(Act a){
		return super.getUla(a);
	}
	public double getUed(Act a){
		return super.getUed(a);
	}
	public double getUld(Act a){
		return super.getUld(a);
	}
	public double getUlegt(Leg l){
		return super.getUlegt(l);
	}
	public double getUlegd(Leg l){
		return super.getUlegd(l);
	}

	public double getDusoc(Act a){
		return dusoc.get(a);
	}
	public double getDudur(Act a){
		return super.getDudur(a);
	}
	public double getDuw(Act a){
		return super.getDuw(a);
	}
	public double getDus(Act a){
		return super.getDus(a);
	}
	public double getDula(Act a){
		return super.getDula(a);
	}
	public double getDued(Act a){
		return super.getDued(a);
	}
	public double getDuld(Act a){
		return super.getDuld(a);
	}
	public double getDulegt(Leg l){
		return super.getDulegt(l);
	}
	public double getDulegd(Leg l){
		return super.getDulegd(l);
	}
	
	/**
	 * Totals the act scores, including socializing during acts, for the entire plan
	 *
	 * @see org.matsim.scoring.super#finish()
	 */
	public void finish() {
		super.finish();
		ActIterator ait = this.plan.getIteratorAct();

		while(ait.hasNext()){
			double temp=0;
			double dtemp=0;
			Act act = (Act)ait.next();
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

