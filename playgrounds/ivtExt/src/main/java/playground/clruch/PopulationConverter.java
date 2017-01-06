package playground.clruch;



/**
 * Created by Claudio on 1/4/2017.
 */


import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.attributable.Attributes;
import playground.clruch.utils.PopulationTools;



public class PopulationConverter {
    public static void main(String[] args) throws MalformedURLException {
        final File dir = new File(args[0]);
        File fileExport = new File(dir, "populationupdated.xml");
        File fileExportGz = new File(dir, "populationupdated.xml.gz");
        File fileImport = new File(dir, "population.xml");

        System.out.println("Is directory?  " + dir.isDirectory());
        // load the existing population file
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(fileImport.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // edit the configuration file - change all modes of transport to "av"
        PopulationTools.changeModesOfTransportToAV(scenario.getPopulation());

        // write the edited population file
        PopulationWriter pw = new PopulationWriter(scenario.getPopulation());
        pw.write(fileExportGz.toString());

        // unzip the new population file
        System.out.println("Opening the gzip file.......................... : "+ fileExportGz.toString());
        GZIPInputStream gzipInputStream = null;
        try{
            gzipInputStream = new GZIPInputStream(new FileInputStream(fileExportGz.toString()));
            OutputStream out = null;
            out = new FileOutputStream(fileExport);

            System.out.println("Transferring bytes from the compressed file to the output file........: Transfer successful");
            byte[] buf = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            System.out.println("The file and stream is ......closing.......... : closed");
            gzipInputStream.close();
            out.close();
        }
        catch (IOException ex){
            System.out.println("Doesn't work"+ex);
        }
    }

}

