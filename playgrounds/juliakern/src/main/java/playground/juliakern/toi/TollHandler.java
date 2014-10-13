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

package playground.juliakern.toi;

import java.util.ArrayList;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;


public class TollHandler implements LinkEnterEventHandler{

	Population pop;
	private ArrayList<Id<Link>> toll10Links;
	private Controler controler;
	private ArrayList<Id<Link>> toll8Links;
	private Double kreu =0.12;
	
	public TollHandler(Population population, Controler controler, Scenario scenario){
		this.controler = controler;
		this.pop=population;
//		tollLinks.add(Id.create("[x=244645.0][y=7032064.1]_[x=244619.01][y=7032045.42] [x=244619.01][y=7032045.42]_[x=244645.0][y=7032064.1]"));
//		tollLinks.add(Id.create("[x=244619.01][y=7032045.42]_[x=243904.37][y=7031498.11] [x=243904.37][y=7031498.11]_[x=244619.01][y=7032045.42]"));
//		tollLinks.add(Id.create("[x=261356.79][y=7030620.86]_[x=260955.8][y=7030667.2] [x=260955.8][y=7030667.2]_[x=261356.79][y=7030620.86]"));
//		tollLinks.add(Id.create("[x=260955.8][y=7030667.2]_[x=260907.6][y=7030666.3] [x=260907.6][y=7030666.3]_[x=260955.8][y=7030667.2]"));
//		tollLinks.add(Id.create("[x=260907.6][y=7030666.3]_[x=260644.02][y=7030635.1] [x=260644.02][y=7030635.1]_[x=260907.6][y=7030666.3]"));
//		tollLinks.add(Id.create("[x=260644.02][y=7030635.1]_[x=260399.0][y=7030549.89] [x=260399.0][y=7030549.89]_[x=260644.02][y=7030635.1]"));
//		tollLinks.add(Id.create("[x=245586.37][y=7032544.01]_[x=245157.1][y=7032347.9] [x=245157.1][y=7032347.9]_[x=245586.37][y=7032544.01]"));
//		tollLinks.add(Id.create("[x=245157.1][y=7032347.9]_[x=244871.85][y=7032207.38] [x=244871.85][y=7032207.38]_[x=245157.1][y=7032347.9]"));
//		tollLinks.add(Id.create("[x=244871.85][y=7032207.38]_[x=244827.6][y=7032182.0] [x=244827.6][y=7032182.0]_[x=244871.85][y=7032207.38]"));
//		tollLinks.add(Id.create("[x=244827.6][y=7032182.0]_[x=244645.0][y=7032064.1] [x=244645.0][y=7032064.1]_[x=244827.6][y=7032182.0]"));
		
		ArrayList<Id<Link>> toll10 = new ArrayList<>();
		toll10.add(Id.create("[x=265109.6][y=7030368.6]_[x=265050.7][y=7030319.5]", Link.class));
		toll10.add(Id.create("[x=265050.7][y=7030319.5]_[x=265109.6][y=7030368.6]", Link.class));
		toll10.add(Id.create("[x=266278.6][y=7030425.6]_[x=266522.2 7030430.3]", Link.class));
		toll10.add(Id.create("[x=266522.2 7030430.3]_[x=266278.6][y=7030425.6]", Link.class));
		toll10.add(Id.create("[x=268073.82][y=7030399.64]_[x=267978.8][y=7030588.3]", Link.class));
		toll10.add(Id.create("[x=267978.8][y=7030588.3]_[x=268073.82][y=7030399.64]", Link.class));
		toll10.add(Id.create("[x=271854.9][y=7032345.79]_[x=271759.1][y=703266.3]", Link.class));
		toll10.add(Id.create("[x=271759.1][y=703266.3]_[x=271854.9][y=7032345.79]", Link.class));
		toll10.add(Id.create("[x=269863.6][y=7036317.5]_[x=269664.1][y=7036438.2]", Link.class));
		toll10.add(Id.create("[x=269664.1][y=7036438.2]_[x=269863.6][y=7036317.5]", Link.class));
		toll10.add(Id.create("[x=269544.47][y=7037860.43]_[x=269573.5][y=7037945.0]", Link.class));
		toll10.add(Id.create("[x=269566.7][y=7037942.9]_[x=269544.47][y=7037860.43]", Link.class));
		toll10.add(Id.create("[x=269790.23][y=7038140.19]_[x=269673.21][y=7037978.88]", Link.class));
		toll10.add(Id.create("[x=269673.21][y=7037978.88]_[x=269790.23][y=7038140.19]", Link.class));
		toll10.add(Id.create("[x=278916.35][y=7041241.32]_[x=278825.2][y=7041185.3]", Link.class));
		toll10.add(Id.create("[x=278825.2][y=7041185.3]_[x=278916.35][y=7041241.32]", Link.class));

		ArrayList<Id<Link>> toll8 = new ArrayList<>();
		toll8.add(Id.create("[x=269149.6][y=7035342.2]_[x=269075.0][y=7035496.5]", Link.class));
		toll8.add(Id.create("[x=269094.0][y=7035229.1]_[x=269073.87][y=7035478.08]", Link.class));
		toll8.add(Id.create("[x=269062.77][y=7035479.52]_[x=269086.46][y=7035232.22]", Link.class));
		toll8.add(Id.create("[x=268948.9][y=7035186.1]_[x=269083.5][y=7035233.0]", Link.class));
		toll8.add(Id.create("[x=270017.09][y=7038240.71]_[x=270211.38][y=7038873.34]", Link.class));
		toll8.add(Id.create("[x=270198.9][y=7038876.8]_[x=270003.63][y=7038246.15]", Link.class));
		toll8.add(Id.create("[x=270358.7][y=7038709.3]_[x=270396.0][y=7038653.6]", Link.class));
		toll8.add(Id.create("[x=270402.38][y=7038656.62]_[x=270358.7][y=7038709.3]", Link.class));
		toll8.add(Id.create("[x=270358.7][y=7038709.3]_[x=270402.38][y=7038656.62]", Link.class));
		toll8.add(Id.create("[x=271406.5][y=7039387.2]_[x=271434.9][y=7039315.9]", Link.class));
		toll8.add(Id.create("[x=271443.7][y=7039318.8]_[x=271420.4][y=7039381.7]", Link.class));
		toll8.add(Id.create("[x=272499.5][y=7039310.6]_[x=272393.2][y=7039291.9]", Link.class));
		toll8.add(Id.create("[x=272384.1][y=7039254.7]_[x=272289.2][y=7039130.0]", Link.class));
		toll8.add(Id.create("[x=272455.6][y=7039291.6]_[x=272406.5][y=7039312.4]", Link.class));
		toll8.add(Id.create("[x=272401.5][y=7039300.2]_[x=272449.7][y=7039284.9]", Link.class));
		toll8.add(Id.create("[x=273580.0][y=7040626.0]_[x=273649.6][y=7040687.5]", Link.class));
		toll8.add(Id.create("[x=273649.6][y=7040687.5]_[x=273580.0][y=7040626.0]", Link.class));
		toll8.add(Id.create("[x=274309.5][y=7041122.3]_[x=274249.4][y=7041133.2]", Link.class));
		toll8.add(Id.create("[x=274249.4][y=7041133.2]_[x=274309.5][y=7041122.3]", Link.class));
		toll8.add(Id.create("[x=274354.93][y=7041532.24]_[x=274375.31]_[x=274375.31][y=7041272.58]", Link.class));
		toll8.add(Id.create("[x=274375.31]_[x=274375.31][y=7041272.58]_[x=274354.93][y=7041532.24]", Link.class));
		toll8.add(Id.create("[x=274718.8][y=7041564.7]_[x=274559.34][y=7041615.25]", Link.class));
		toll8.add(Id.create("[x=274559.34][y=7041615.25]_[x=274718.8][y=7041564.7]", Link.class));
		toll8.add(Id.create("[x=269533.2][y=7039088.7]_[x=269414.41][y=7038755.84]", Link.class));
		toll8.add(Id.create("[x=269414.41][y=7038755.84]_[x=269533.2][y=7039088.7]", Link.class));
		toll8.add(Id.create("[x=268337.29][y=7039168.81]_[x=268298.22][y=7039005.38]", Link.class));
		toll8.add(Id.create("[x=268298.22][y=7039005.38]_[x=268337.29][y=7039168.81]", Link.class));
		toll8.add(Id.create("[x=267578.92][y=7039135.92]_[x=267958.1][y=7039693.7]", Link.class));
		toll8.add(Id.create("[x=267958.1][y=7039693.7]_[x=267578.92][y=7039135.92]", Link.class));
		
		this.toll10Links = new ArrayList<>();
		this.toll8Links = new ArrayList<>();
		
		Set<Id<Link>> networklinks = scenario.getNetwork().getLinks().keySet();
		
		for(Id<Link> linkId: 	networklinks){
			for(Id<Link> toll8id: toll8){
				if(toll8id.compareTo(linkId)==0){
					toll8Links.add(linkId);
					break;
				}
			}
			for(Id<Link> toll10id: toll10){
				if(toll10id.compareTo(linkId)==0){
					toll10Links.add(linkId);
					break;
				}
			}
		}
	}
	@Override
	public void reset(int iteration) {


	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		if(toll10Links.contains(event.getLinkId())){
			Double time = event.getTime();
			Double amount =0.0;
			Id<Person> agentId = Id.create(event.getAttributes().get("person"), Person.class);
			amount =-10*kreu;
			if(time >= 7*60*60 && time <= 9*60*60){
				amount = -20 * kreu;
			}else{
				if(time>=15*60*60 && time <= 17*60*60){
					amount = -20*kreu;
				}
			}
			PersonMoneyEvent moneyEvent = new PersonMoneyEvent(time, agentId, amount);
			controler.getEvents().processEvent(moneyEvent);
		}
		
		if(toll8Links.contains(event.getLinkId())){
			Double time = event.getTime();

			if(time >= 7*60*60 && time <= 9*60*60){
				Double amount =0.0;
				Id<Person> agentId = Id.create(event.getAttributes().get("person"), Person.class);
				amount = -8 * kreu;
				PersonMoneyEvent moneyEvent = new PersonMoneyEvent(time, agentId, amount);
				controler.getEvents().processEvent(moneyEvent);
			}else{
				if(time>=15*60*60 && time <= 17*60*60){
					Id<Person> agentId = Id.create(event.getAttributes().get("person"), Person.class);
					Double amount = -8*kreu;
					PersonMoneyEvent moneyEvent = new PersonMoneyEvent(time, agentId, amount);
					controler.getEvents().processEvent(moneyEvent);
				}
			}

		}
		
	}

	
	/*
	 * 
2014-02-28 18:21:11,413  INFO ShapeConverterNetwork:158 toll roads [x=244645.0][y=7032064.1]_[x=244619.01][y=7032045.42] [x=244619.01][y=7032045.42]_[x=244645.0][y=7032064.1]
2014-02-28 18:21:11,414  INFO ShapeConverterNetwork:158 toll roads [x=244619.01][y=7032045.42]_[x=243904.37][y=7031498.11] [x=243904.37][y=7031498.11]_[x=244619.01][y=7032045.42]
2014-02-28 18:21:11,581  INFO ShapeConverterNetwork:158 toll roads [x=261356.79][y=7030620.86]_[x=260955.8][y=7030667.2] [x=260955.8][y=7030667.2]_[x=261356.79][y=7030620.86]
2014-02-28 18:21:11,582  INFO ShapeConverterNetwork:158 toll roads [x=260955.8][y=7030667.2]_[x=260907.6][y=7030666.3] [x=260907.6][y=7030666.3]_[x=260955.8][y=7030667.2]
2014-02-28 18:21:11,583  INFO ShapeConverterNetwork:158 toll roads [x=260907.6][y=7030666.3]_[x=260644.02][y=7030635.1] [x=260644.02][y=7030635.1]_[x=260907.6][y=7030666.3]
2014-02-28 18:21:11,583  INFO ShapeConverterNetwork:158 toll roads [x=260644.02][y=7030635.1]_[x=260399.0][y=7030549.89] [x=260399.0][y=7030549.89]_[x=260644.02][y=7030635.1]
2014-02-28 18:21:11,732  INFO ShapeConverterNetwork:158 toll roads [x=245586.37][y=7032544.01]_[x=245157.1][y=7032347.9] [x=245157.1][y=7032347.9]_[x=245586.37][y=7032544.01]
2014-02-28 18:21:11,733  INFO ShapeConverterNetwork:158 toll roads [x=245157.1][y=7032347.9]_[x=244871.85][y=7032207.38] [x=244871.85][y=7032207.38]_[x=245157.1][y=7032347.9]
2014-02-28 18:21:11,733  INFO ShapeConverterNetwork:158 toll roads [x=244871.85][y=7032207.38]_[x=244827.6][y=7032182.0] [x=244827.6][y=7032182.0]_[x=244871.85][y=7032207.38]
2014-02-28 18:21:11,734  INFO ShapeConverterNetwork:158 toll roads [x=244827.6][y=7032182.0]_[x=244645.0][y=7032064.1] [x=244645.0][y=7032064.1]_[x=244827.6][y=7032182.0]
	 */
}
