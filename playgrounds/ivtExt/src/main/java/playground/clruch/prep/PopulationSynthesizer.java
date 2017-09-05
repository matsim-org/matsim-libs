// code by clruch
package playground.clruch.prep;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.util.GZHandler;

/**
 * Created by Claudio on 1/4/2017.
 */
@Deprecated // TODO this class is not used
/* package */ class PopulationSynthesizer {
    public static void main(String[] args) throws MalformedURLException {
        final File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            new RuntimeException("not a directory: " + dir).printStackTrace();
            System.exit(-1);
        }
        final File fileImport = new File(dir, "populationupdated.xml");
        final File fileExport = new File(dir, "populationsynthetic.xml");
        final File fileExportGz = new File(dir, "populationsynthetic.xml.gz");

        // load the existing population file
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(fileImport.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        final Population population = scenario.getPopulation();

        // Limit the population to some amount
        TheApocalypse.decimatesThe(population).toNoMoreThan(5000).people();


        // generate generic population in city center
        Population populationEnhanced = addGenericPeople(population);

        CreateTraffic creator = new CreateTraffic();
        creator.init(dir+"/av_config.xml");
        creator.create();
        creator.write();


        {
            // write the modified population to file
            PopulationWriter pw = new PopulationWriter(populationEnhanced);
            pw.write(fileExportGz.toString());
        }

        // extract the created .gz file
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Population addGenericPeople(Population population){
        List<Id<Person>> list = new ArrayList<>(population.getPersons().keySet());
        Id<Person> p1 = list.get(0);
        System.out.println(p1.toString());
        
        // the map is not used:
        // HashMap<Id<Person>,Person> popmap = (HashMap<Id<Person>, Person>) population.getPersons();


        /*
        Collections.shuffle(list, new Random(7582456789l));
        final int sizeAnte = list.size();
        list.stream() //
                .limit(Math.max(0, sizeAnte - capacityOfArk)) //
                .forEach(population::removePerson);
        final int sizePost = population.getPersons().size();
        GlobalAssert.that(sizePost <= capacityOfArk);
        return this;
        */


        return population;
    }



}
