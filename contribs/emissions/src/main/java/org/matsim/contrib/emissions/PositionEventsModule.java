package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.log4j.Logger;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * start with a new module, copy everything that is needed and try to unify later
 */
public class PositionEventsModule extends AbstractModule {

    private static final Logger log = Logger.getLogger(PositionEventsModule.class);

    @Inject
    private EmissionsConfigGroup config;


    @Override
    public void install() {

        if (!config.isWritingEmissionsEvents())
            bind(EventsManager.class).to(EventsUtils.getDefaultEventsManagerClass());


    }

    static class HbefaTableLoader {

        private static HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {

            if (string.endsWith("Freeflow")) return HbefaTrafficSituation.FREEFLOW;
            else if (string.endsWith("Heavy")) return HbefaTrafficSituation.HEAVY;
            else if (string.endsWith("Satur.")) return HbefaTrafficSituation.SATURATED;
            else if (string.endsWith("St+Go")) return HbefaTrafficSituation.STOPANDGO;
            else if (string.endsWith("St+Go2")) return HbefaTrafficSituation.STOPANDGO_HEAVY;
            else {
                log.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in hbefa input file.");
                throw new RuntimeException();
            }
        }

        Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> load(URL file) {

            Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgWarmTable = new HashMap<>();

            try (var reader = IOUtils.getBufferedReader(file);
                 var parser = CSVParser.parse(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader())) {

                for (var record : parser) {
                    var key = new HbefaWarmEmissionFactorKey();

                    //vehicle category
                    var vehicleCategory = EmissionUtils.mapString2HbefaVehicleCategory(record.get("VehCat"));
                    var pollutant = EmissionUtils.getPollutant(record.get("Component"));
                    var trafficSit = record.get("TrafficSit");
                    var roadCategory = trafficSit.substring(0, trafficSit.lastIndexOf('/'));
                    var trafficSituation = mapString2HbefaTrafficSituation(trafficSit);

                    key.setHbefaVehicleCategory(vehicleCategory);
                    key.setHbefaComponent(pollutant);
                    key.setHbefaRoadCategory(roadCategory);
                    key.setHbefaTrafficSituation(trafficSituation);
                    key.setHbefaVehicleAttributes(new HbefaVehicleAttributes());

                    var value = new HbefaWarmEmissionFactor();
                    value.setSpeed(Double.parseDouble(record.get("V_weighted")));
                    value.setWarmEmissionFactor(Double.parseDouble("EFA_weighted"));

                    avgWarmTable.put(key, value);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return avgWarmTable;
        }
    }
}
