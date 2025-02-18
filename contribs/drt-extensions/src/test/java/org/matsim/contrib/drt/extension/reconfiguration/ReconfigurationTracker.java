package org.matsim.contrib.drt.extension.reconfiguration;

import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.passenger.DefaultDvrpLoadFromTrip;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

class ReconfigurationTracker implements PersonEntersVehicleEventHandler {
    private final Population population;

    ReconfigurationTracker(Population population) {
        this.population = population;
    }

    public int pickedUpPassengers = 0;
    public int pickedUpGoods = 0;

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Person person = population.getPersons().get(event.getPersonId());

        if (person != null) {
            String load = (String) person.getAttributes().getAttribute(DefaultDvrpLoadFromTrip.LOAD_ATTRIBUTE);

            if (load.contains("passengers")) {
                pickedUpPassengers++;
            } else if (load.contains("goods")) {
                pickedUpGoods++;
            }
        }
    }

    @Override
    public void reset(int iteration) {
        pickedUpPassengers = 0;
        pickedUpGoods = 0;
    }

    static public ReconfigurationTracker install(Controler controller) {
        ReconfigurationTracker tracker = new ReconfigurationTracker(controller.getScenario().getPopulation());

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(tracker);
            }
        });

        return tracker;
    }
}
