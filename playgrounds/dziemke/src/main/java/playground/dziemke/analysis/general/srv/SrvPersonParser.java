package playground.dziemke.analysis.general.srv;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author gthunig on 30.03.2017.
 */
public class SrvPersonParser {
    private final static Logger log = Logger.getLogger(SrvPersonParser.class);

    //class atributes
    private Map<String,Integer> columnNumbers;

    private Population population;

    public SrvPersonParser() {
        population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
    }

    public Population parse(String srvPersonFilePath) {

        int lineCount = 0;

        try {
            BufferedReader bufferedReader = IOUtils.getBufferedReader(srvPersonFilePath);

            // header
            String currentLine = bufferedReader.readLine();
            lineCount++;
            String[] heads = currentLine.split(";", -1);
            columnNumbers = new LinkedHashMap<>(heads.length);
            for (int i = 0; i < heads.length; i++) {
                columnNumbers.put(heads[i],i);
            }


            // read data
            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] entries = currentLine.split(";", -1);

                parseAndAddPerson(entries);

                lineCount++;

                if (lineCount % 100000 == 0) {
                    log.info(lineCount+ " lines read in so far.");
                    Gbl.printMemoryUsage();
                }
            }
        } catch (IOException e) {
            log.error(new Exception(e));
        }

        return this.population;
    }

    private void parseAndAddPerson(String[] entries) {

        Id<Household> householdId = Id.create(entries[columnNumbers.get(SrvPersonUtils.HOUSEHOLD_ID)], Household.class);
        Id<Person> personId = Id.create(entries[columnNumbers.get(SrvPersonUtils.PERSON_ID)], Person.class);

        int age = new Integer(entries[columnNumbers.get(SrvPersonUtils.AGE)]);
        int sex = new Integer(entries[columnNumbers.get(SrvPersonUtils.SEX)]);
        int employed = new Integer(entries[columnNumbers.get(SrvPersonUtils.EMPLOYED)]);
        int driversLicence = new Integer(entries[columnNumbers.get(SrvPersonUtils.DRIVERS_LICENCE)]);
        double weight = Double.parseDouble(entries[columnNumbers.get(SrvPersonUtils.WEIGHT)]);

        Id<Person> uniquePersonId = Srv2MatsimPopulationUtils.createUniquePersonId(householdId, personId);

        Person person = population.getFactory().createPerson(Id.create(uniquePersonId, Person.class));

        SrvPersonUtils.setWeight(person, weight);

        if (age >= 0) {
            PersonUtils.setAge(person, age);
        } else {
            log.warn("Age is not a positive number.");
        }

        if (sex == 1) {
            PersonUtils.setSex(person, "male");
        } else if (sex == 2) {
            PersonUtils.setSex(person, "female");
        } else {
            log.warn("Sex is neither male nor female.");
        }

        if (employed >= 8 && employed <= 11) {
            PersonUtils.setEmployed(person, true);
        } else if (employed >= 1 && employed <= 7 || employed == 12) {
            PersonUtils.setEmployed(person, false);
        } else {
            log.warn("No information on employment.");
        }

        if (employed == 7) {
            SrvPersonUtils.setStudent(person, true);
        } else if (employed == -9 || employed == -10) {
            log.warn("No information on being student.");
        } else {
            SrvPersonUtils.setStudent(person, false);
        }

        if (driversLicence == 1) {
            PersonUtils.setLicence(person, "yes");
        } else {
            PersonUtils.setLicence(person, "no");
        }

        this.population.addPerson(person);
    }

}
