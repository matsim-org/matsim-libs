package playground.clruch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.prep.NetworkCutClean;
import playground.clruch.prep.PopulationTools;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.utils.GZHandler;
import playground.clruch.utils.GlobalAssert;


public class ScenarioPreparer {

    public static void main(String[] args) throws MalformedURLException, Exception {
        
        // INPUT ARGUMENT: path to a config.xml file which contains references to the original xml files (population.xml)
        // if the xml file with the converted files is supplied, no changes are implemented by the ScenarioPreparer. 
        System.out.println("converting simulation data files from " + args[0]);


        // BEGIN: CUSTOMIZE ----------------------------------------------- 
        // set manually depending on the scenario:
        final int maxPopulationSize = 2000;
        final int numVirtualNodes = 5;
        final int dtTravelData = 500;
        final boolean completeGraph = true;

        
        // cutting of scenario to circle
        // increasing the first value goes right        
        // increasing the second value goes north
        // Zurich
        // final Coord center = new Coord(2683600.0, 1251400.0);
        // final double radius = 10000; // (set to -1 for no cutting)
        // Basel
        // final Coord center = new Coord(2612859.0,1266343.0);
        // final double radius = 12000; // (set to -1 for no cutting)
        // Sioux
        final Coord center = new Coord(678365.311581,4827050.237694);
        final double radius = 50000;

        final boolean populationeliminateFreight = true;
        final boolean populationchangeModeToAV = true;
        final boolean populationeliminateWalking = true;
         

        // output file names
        final String VIRTUALNETWORKFILENAME = "virtualNetwork";
        final String TRAVELDATAFILENAME = "travelData";
        final String NETWORKUPDATEDNAME = "networkConverted";
        final String POPULATIONUPDATEDNAME = "populationConverted";
        

        // END: CUSTOMIZE -------------------------------------------------

        File configFile = new File(args[0]);
        File dir = configFile.getParentFile();

        // 0) load files
        Config config = ConfigUtils.loadConfig(configFile.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();

        // 1) cut network (and reduce population to new network)
        {
            NetworkCutClean.elminateOutsideRadius(network, center, radius);
            final File fileExportGz = new File(dir, NETWORKUPDATEDNAME + ".xml.gz");
            final File fileExport = new File(dir, NETWORKUPDATEDNAME + ".xml");
            {
                // write the modified population to file
                NetworkWriter nw = new NetworkWriter(network);
                nw.write(fileExportGz.toString());
            }
            // extract the created .gz file
            try {
                GZHandler.extract(fileExportGz, fileExport);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("saved converted network to: " + dir + NETWORKUPDATEDNAME + ".xml");
        }

        // 2) adapt the population to new network
        {
            System.out.println("Original population size: " + population.getPersons().values().size());
            PopulationTools.elminateOutsideNetwork(population, network);
            System.out.println("Population size after radius cut: " + population.getPersons().values().size());
            if(populationeliminateFreight) PopulationTools.eliminateFreight(population);
            System.out.println("Population size after removing freight: " + population.getPersons().values().size());
            if(populationeliminateWalking) PopulationTools.eliminateWalking(population);
            System.out.println("Population size after removing walking people: " + population.getPersons().values().size());
            if(populationchangeModeToAV) PopulationTools.changeModesOfTransportToAV(population);
            System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
            PopulationTools.changeModesOfTransportToAV(population);
            System.out.println("Population size after conversion to mode AV:" + population.getPersons().values().size());
            TheApocalypse.decimatesThe(population).toNoMoreThan(maxPopulationSize).people();
            System.out.println("Population after decimation:" + population.getPersons().values().size());
            GlobalAssert.that(population.getPersons().size()>0);
            
            final File fileExportGz = new File(dir, POPULATIONUPDATEDNAME + ".xml.gz");
            final File fileExport = new File(dir, POPULATIONUPDATEDNAME + ".xml");

            {
                // write the modified population to file
                PopulationWriter pw = new PopulationWriter(population);
                pw.write(fileExportGz.toString());
            }

            // extract the created .gz file
            try {
                GZHandler.extract(fileExportGz, fileExport);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 2) create virtual Network
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        VirtualNetwork virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes,completeGraph);
        VirtualNetworkIO.toByte(new File(dir + "/virtualNetwork/" + VIRTUALNETWORKFILENAME), virtualNetwork);
        VirtualNetworkIO.toXML(dir + "/virtualNetwork/" + VIRTUALNETWORKFILENAME+".xml", virtualNetwork);
        System.out.println("saved virtual network byte format to : "+ new File(dir + "/virtualNetwork/" + VIRTUALNETWORKFILENAME));
        
        
        
        // 3) generate travelData
        TravelData travelData = new TravelData(virtualNetwork, network, scenario.getPopulation(), dtTravelData);
        TravelDataIO.toByte(new File(dir+"/virtualNetwork/"+TRAVELDATAFILENAME), travelData);
        System.out.println("saved travelData byte format to : "+ new File(dir+"/virtualNetwork/"+TRAVELDATAFILENAME));

        
        System.out.println("successfully converted simulation data files from " + args[0]);
    }
}

