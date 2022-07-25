package org.matsim.urbanEV.analysis;


import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jcodec.common.Tuple;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.contrib.ev.stats.ChargerOccupancyXYDataProvider;
import org.matsim.contrib.ev.stats.ChargerPowerCollector;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class ChargerToXY implements ChargingEndEventHandler, ChargingStartEventHandler, MobsimBeforeCleanupListener
{

    @Inject
    OutputDirectoryHierarchy controlerIO;
    @Inject
    IterationCounter iterationCounter;
    @Inject
    Scenario scenario;

    private final ChargingInfrastructureSpecification chargingInfrastructureSpecification;
    private final Network network;
    Map<Id<Charger>, List<Id<ElectricVehicle>>> crtChargers = new HashMap<>();
    static List<XYDataContainer> dataContainers = new ArrayList<>();
/*
* This class collects the XY-Coordinates of every charger. During every iteration it monitors the charging vehicles of every charger.
* @author Jonas116
 */

@Inject
public ChargerToXY (ChargingInfrastructureSpecification chargingInfrastructureSpecification, Network network, Scenario scenario){
    this.chargingInfrastructureSpecification = chargingInfrastructureSpecification;
    this.network = network;
    this.scenario = scenario;
    for (Id<Charger> chargerId : chargingInfrastructureSpecification.getChargerSpecifications().keySet()) {
        XYDataContainer dataContainer = new XYDataContainer(0, chargerId, chargingInfrastructureSpecification.getChargerSpecifications().get(chargerId).getLinkId(),0);
        dataContainers.add(dataContainer);
    }

}
public static List<XYDataContainer> getDataContainers(){ return dataContainers; }




    @Override
    public void handleEvent(ChargingEndEvent event) {

    crtChargers.get(event.getChargerId()).remove(event.getVehicleId());
        XYDataContainer dataContainer = new XYDataContainer(event.getTime(),
                event.getChargerId(),
                chargingInfrastructureSpecification.getChargerSpecifications().get(event.getChargerId()).getLinkId(),
                crtChargers.get(event.getChargerId()).size());
        this.dataContainers.add(dataContainer);


    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
       this.crtChargers.compute(event.getChargerId(), (person, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(event.getVehicleId());
            return list;
        });
        XYDataContainer dataContainer = new XYDataContainer(event.getTime(),
                event.getChargerId(),
                chargingInfrastructureSpecification.getChargerSpecifications().get(event.getChargerId()).getLinkId(),
                crtChargers.get(event.getChargerId()).size());
        this.dataContainers.add(dataContainer);

    }


    @Override
    public void reset(int iteration) {

    crtChargers.clear();
    dataContainers.clear();
    }

    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent mobsimBeforeCleanupEvent) {
        CSVPrinter csvPrinter = null;

        try {
            csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "chargerXYData.csv"))), CSVFormat.DEFAULT.withDelimiter(';').
                    withHeader("X", "Y", "Time", "ChargerId", "chargingVehicles"));
            List<ChargerToXY.XYDataContainer> dataContainers = ChargerToXY.getDataContainers();

            for (ChargerToXY.XYDataContainer dataContainer : dataContainers) {

                csvPrinter.printRecord(dataContainer.x, dataContainer.y, Time.writeTime(dataContainer.time), dataContainer.chargerId, dataContainer.chargingVehicles);
            }
            csvPrinter.close();

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }


    public class XYDataContainer{
       final double x;
       final double y;
       final double time;
       final Id<Charger> chargerId;
       final int chargingVehicles;


        public XYDataContainer(double time, Id<Charger> chargerId, Id<Link> linkId, int chargingVehicles) {
            Coord coord = new Coord(network.getLinks().get(linkId).getToNode().getCoord().getX(), network.getLinks().get(linkId).getToNode().getCoord().getY());
            this.x = coord.getX();
            this.y = coord.getY();
            this.time = time;
            this.chargerId = chargerId;
            this.chargingVehicles = chargingVehicles;
        }


        }
    }














