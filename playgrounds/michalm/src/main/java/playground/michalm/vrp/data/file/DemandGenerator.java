package playground.michalm.vrp.data.file;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.xml.parsers.*;

import org.apache.log4j.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.scenario.*;
import org.xml.sax.*;

import cern.jet.random.*;
import cern.jet.random.engine.*;


public class DemandGenerator
{
    private static final Logger log = Logger.getLogger(DemandGenerator.class);

    private Scenario scenario;

    public DemandGenerator(String networkFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException

    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFileName);
    }


    private int id = 0;


    public void generate()
    {
        Population popul = scenario.getPopulation();
        PopulationFactory pf = popul.getFactory();
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();

        List<Link> links = new ArrayList<Link>(network.getLinks().values());
        int linkCount = links.size();
        int planCount = 100;
        
        Uniform uniform = new Uniform(new MersenneTwister(new Date()));


        for (int p = 0; p < planCount; p++) {
            Person person = pf.createPerson(scenario.createId(Integer.toString(id++)));
            Plan plan = pf.createPlan();

            Link link = links.get(uniform.nextIntFromTo(0, linkCount - 1));
            Activity act = pf.createActivityFromLinkId("dummy", link.getId());
            act.setEndTime(uniform.nextIntFromTo(0, 23 * 60 * 60));
            plan.addActivity(act);

            plan.addLeg(pf.createLeg("taxi"));

            link = links.get(uniform.nextIntFromTo(0, linkCount - 1));
            act = pf.createActivityFromLinkId("dummy", link.getId());
            plan.addActivity(act);

            person.addPlan(plan);
            popul.addPerson(person);
        }
    }


    public void write(String plansfileName)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
                .writeV4(plansfileName);
        log.info("Generated population written to: " + plansfileName);
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName;

        String networkFileName;
        String plansFileName;
        String idField;

        dirName = "D:\\PP-rad\\taxi\\siec1\\";
        networkFileName = dirName + "network.xml";
        plansFileName = dirName + "plans.xml";
        idField = "ID";

        DemandGenerator dg = new DemandGenerator(networkFileName, idField);
        dg.generate();
        dg.write(plansFileName);
    }
}
