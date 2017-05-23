package playground.dziemke.analysis.general.srv;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import java.util.List;

/**
 * @author gthunig on 30.03.2017.
 */
public class Srv2MATSimPopulation {
    private final static Logger log = Logger.getLogger(Srv2MATSimPopulation.class);

    private Population population;
    private List<FromSrvTrip> trips;

    public Srv2MATSimPopulation(String srvPersonFilePath, String srvTripFilePath) {
        parse(srvPersonFilePath, srvTripFilePath);
    }

    private void parse(String srvPersonFilePath, String srvTripFilePath) {
        SrvPersonParser personParser = new SrvPersonParser();
        population = personParser.parse(srvPersonFilePath);

        SrvTripParser planParser = new SrvTripParser(population);
        population = planParser.parse(srvTripFilePath);
        trips = planParser.getTrips();
    }

    public void writePopulation(String outputPopulationFilePath) {
        PopulationWriter writer = new PopulationWriter(population);
        writer.write(outputPopulationFilePath);
    }

    public Population getPopulation() {
        return population;
    }

    public List<FromSrvTrip> getTrips() {
        return trips;
    }
}
