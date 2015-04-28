/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.energy;

import java.util.HashMap;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.energy.charging.ChargingHandler;

public class ElectricCBMain
{
    private Config config;
    private Scenario scenario;
    private HashMap<Id<Vehicle>, Vehicle> bevs = new HashMap<>();
    private final EnergyConsumptionModel ecm = new EnergyConsumptionModelRicardoFaria2012();

    public static void main(String[] args)
    {
      ElectricCBMain ec = new ElectricCBMain();
      
      ec.run();
    }

    private void run()
    {
        Config config = ConfigUtils.createConfig();
        ConfigUtils.loadConfig(config, "C:/local_jb/cottbus/config.xml");
        this.scenario = ScenarioUtils.loadScenario(config);
        
        ChargingHandler ch = new ChargingHandler();
//        for (Person person : scenario.getPopulation().getPersons().values()){
//            Id<Person> personId = person.getId();
//            String pNoString = personId.toString().split("_")[0];
//            if (pNoString.endsWith("00")){
//                
//                Id<Vehicle> vid =  Id.create(personId, Vehicle.class);
//                BatteryElectricVehicle bev = new BatteryElectricVehicleImpl(ecm, 20*1000*3600,vid);
//                this.bevs.put(vid, bev);
//                
//                Activity act = (Activity)person.getPlans().get(0).getPlanElements().get(2);
//                Id<Link> linkId = act.getLinkId();
//                Id<Charger> chargerId = Id.create(linkId+"_"+pNoString, Charger.class);
//                ChargerImpl c = new ChargerImpl(chargerId, linkId, 4, 1);
//                ch.addCharger(c);
//                
//            }
//            
//        }
//        ch.addVehicles(bevs);
        EnergyConsumptionTracker ect = new EnergyConsumptionTracker(bevs, scenario.getNetwork());
        final EVehQSimFactory ev = new EVehQSimFactory(ch,ect);
        
        final Controler c = new Controler(scenario);
        c.setOverwriteFiles(true);
        c.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                if (getConfig().controler().getMobsim().equals("eveh")) {
                    bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
                        @Override
                        public Mobsim get() {
                            return ev.createMobsim(c.getScenario(), c.getEvents());
                        }
                    });
                }
            }
        });
        config.controler().setMobsim("eveh");
        c.run();
        ch.getChargerLog().writeToFiles(config.controler().getOutputDirectory());
        ch.getSoCLog().writeToFiles(config.controler().getOutputDirectory());
        
        
    }
    


}
