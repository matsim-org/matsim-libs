package playground.johannes.synpop.data.io.flattable;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;

import java.io.IOException;
import java.util.Set;

/**
 * Created by johannesillenberger on 05.04.17.
 */
public class XML2Table {

    private static final Logger logger = Logger.getLogger(XML2Table.class);

    public static void main(String args[]) throws IOException {
        String inFile = args[0];
        String baseFile = args[1];

        logger.info("Loading population...");
        Set<? extends Person> persons = PopulationIO.loadFromXML(inFile, new PlainFactory());
        logger.info("Writing tables...");
        PopulationWriter.write(persons, baseFile);
        logger.info("Done.");
    }
}
