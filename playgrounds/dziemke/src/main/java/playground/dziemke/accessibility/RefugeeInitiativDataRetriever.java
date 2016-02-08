package playground.dziemke.accessibility;

import com.vividsolutions.jts.geom.Coordinate;

import playground.dziemke.accessibility.ptmatrix.CSVFileWriter;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gthunig, 03.02.16
 *
 * This class searches for data about refugee initiatives on the berlin.de website and writes them into an output file.
 */
public class RefugeeInitiativDataRetriever {

    private static final Logger log = Logger.getLogger(RefugeeInitiativDataRetriever.class);

    private static final String WEBSITE_URL = "https://www.berlin.de/fluechtlinge/berlin-engagiert-sich/berliner-initiativen/";

    private static final String outputFile = "refugeeInitiativData.csv";

    public static void main(String[] args) {
        try {
            URL url = new URL(WEBSITE_URL);
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                log.info("Reading " + WEBSITE_URL);

                Map<String, Coordinate> refugeeInitiatives = new HashMap<>();

                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("mapOptions.markerList.push({")) {
                            String title = null;
                            Coordinate coordinate = null;
                            String[] lineArray;
                            line = bufferedReader.readLine();
                            if (line.contains("coordinates")) {
                                lineArray = line.split("'");
                                String[] coordStrings = lineArray[1].split(",");
                                double lat = Double.parseDouble(coordStrings[0]);
                                double lon = Double.parseDouble(coordStrings[1]);
                                coordinate = new Coordinate(lat, lon);
                            }
                            bufferedReader.readLine();
                            bufferedReader.readLine();
                            bufferedReader.readLine();
                            bufferedReader.readLine();
                            line = bufferedReader.readLine();
                            if (line.contains("title")) {
                                lineArray = line.split("'");
                                title = lineArray[1];
                            }
                            if (title != null && coordinate != null) {
                                refugeeInitiatives.put(title, coordinate);
                            }
                        }
                    }
                    writeRefugeeData(refugeeInitiatives, outputFile);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void writeRefugeeData(Map<String, Coordinate> refugeeInitiatives, String outputFile) {
        log.info("Writing refugee data to " + outputFile);

        CSVFileWriter writer = new CSVFileWriter(outputFile, ";");

        writeInitialLine(writer);
        for (Map.Entry<String, Coordinate> entry : refugeeInitiatives.entrySet()) {
            writer.writeField(entry.getKey());
            writer.writeField(entry.getValue().x);
            writer.writeField(entry.getValue().y);
            writer.writeNewLine();
        }
        writer.close();

        log.info("Writing refugee data: done");
    }

    private static void writeInitialLine(CSVFileWriter writer) {
        writer.writeField("Title");
        writer.writeField("Lat");
        writer.writeField("Lon");
        writer.writeNewLine();
    }
}