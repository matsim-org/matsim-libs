package commercialtraffic.examples;/* *********************************************************************** *
				* project: org.matsim.*
				*                                                                         *
				* *********************************************************************** *
				*                                                                         *
				* copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;
/**
 * @author thiel
 */
public class AttributesParcelExample {

	public static void main(String[] args) {
		Config config = createConfig();
		Scenario scenario = createScenario(config);
		String personsFile = "C:\\Users\\VW7N5TD\\Desktop\\Programme\\MatSim\\02_Freight\\Input\\Plans\\finishedPlans_0.01.xml.gz";

		new PopulationReader(scenario).readFile(personsFile);

		int deliveryCounter = 0;
		int personCounter = 0;
		//Servicezeiten des Unternehmens
		int deliveryBusinessStart = 9 * 3600;
		int deliveryBusinessEnd = 18 * 3600;
		//TODO random Verteilung der Zustellungen 
		double deliveryRate=0.1;
		
		//Sollte so umgesetzt werden, dass man die einzelnen Attribute der Pakete festlegen kann (Volumen, Gewicht,...)
		String parcelSmall ="smallParcel";
//		String parcelMid ="midParcel";
//		String parcelBig ="BigParcel";
		

		for (Person p : scenario.getPopulation().getPersons().values()) {
			personCounter++;
			for (Plan plan : p.getPlans()) {
				double r = Math.random();
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						//Prüfe ob aktuelle Activität die erste ist
						boolean firstAct = false;
						if (act == PopulationUtils.getFirstActivity(plan)) {
							firstAct = true;
						}
						//Wenn Home-Activity erste Aktivität ist hat diese auch eine End-Time
						if (act.getType().startsWith("home") && r <= deliveryRate) {
							if (act.getEndTime() > deliveryBusinessStart && firstAct) {
								act.getAttributes().putAttribute("deleteActivity", "None");
								act.getAttributes().putAttribute("deliveryAmount", "1");								
								act.getAttributes().putAttribute("deliveryType", parcelSmall);
								act.getAttributes().putAttribute("deliveryTimeStart", (deliveryBusinessStart));
								act.getAttributes().putAttribute("deliveryTimeEnd",
										Math.min(deliveryBusinessEnd, act.getEndTime()));
								deliveryCounter++;
								
								break;
							}
							//Wenn Home-Actiovity nicht erste Act. dann evtl. End-Time=-inf -> ANpassung der Delivery zeiten
							else if (firstAct == false) {
								Leg prevLeg = PopulationUtils.getPreviousLeg(plan, act);
								Activity prevAct = PopulationUtils.getPreviousActivity(plan, prevLeg);
								double prevActEndtime = prevAct.getEndTime();
								if (prevAct.getEndTime() < deliveryBusinessEnd) {
									act.getAttributes().putAttribute("deleteActivity", "None");
									act.getAttributes().putAttribute("deliveryAmount", "1");
									act.getAttributes().putAttribute("deliveryType", parcelSmall);
									act.getAttributes().putAttribute("deliveryTimeStart",
											Math.max(deliveryBusinessStart, prevActEndtime));
									
									if (act.getEndTime() < 0) {
										act.getAttributes().putAttribute("deliveryTimeEnd", deliveryBusinessEnd);
										deliveryCounter++;
										break;
									} else {
										act.getAttributes().putAttribute("deliveryTimeEnd",
												Math.min(act.getEndTime(), deliveryBusinessEnd));
										deliveryCounter++;
										break;
									}
								}
							}
						}
					}

				}
			}
		}

		System.out.println("Es werden:" + deliveryCounter + " von " + personCounter + " beliefert.");
		new PopulationWriter(scenario.getPopulation())
				.write("C:\\Users\\VW7N5TD\\Desktop\\Programme\\MatSim\\02_Freight\\Attributes_dummy\\parcels.xml.gz");

	}
}
