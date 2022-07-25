package playground.vsp.ev;


import com.google.common.collect.Iterables;
import com.opencsv.exceptions.CsvException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static java.lang.Double.parseDouble;

public class CSVToXML {
    /*
    This class was made to collect the coordinates of an official csv of the chargers in Berlin. Since this was not used, this can be deleted.
    @Jonas116
     */

    private static final String COMMA_DELIMITER = ";";
    public String fileName;
    List<ChargerSpecification> chargers = new ArrayList<>();

    public CSVToXML(String fileName, Network network) throws IOException, CsvException {

        List<List<String>> records = new ArrayList<>();


        //matsim-berlin is in DHDN-GK4 (EPSG:31468)
        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);


        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                records.add(Arrays.asList(values));
            }


            }
            List<Link> chargerLinks = new ArrayList<>();
            for (List<String> record : Iterables.skip(records, 2)) {

                double x = Double.parseDouble(record.get(0));
                double y = Double.parseDouble(record.get(1));
                Coord coord = transformer.transform(new Coord(x,y));

               Link chargerLink = NetworkUtils.getNearestLink(network,coord);
               if (!chargerLinks.contains(chargerLink)){
                   chargerLinks.add(chargerLink);
                   ImmutableChargerSpecification.Builder builder = ImmutableChargerSpecification.newBuilder();

                   if(record.get(7).equals("43")||Math.round(parseDouble(record.get(7)))>=50){
                       chargers.add(builder
                               .linkId(Id.createLinkId(chargerLink.getId()))
                               .id(Id.create("charger" + record.get(11), Charger.class))
                               .chargerType("DC")
                               .plugCount(5)
                               .plugPower((int) Math.round(parseDouble(record.get(7))*1000))
                               .build());
                   }
                   else{
                       chargers.add(builder
                               .linkId(Id.createLinkId(chargerLink.getId()))
                               .id(Id.create("charger" + record.get(11), Charger.class))
                               .chargerType("AC")
                               .plugCount(5)
                               .plugPower((int) Math.round(parseDouble(record.get(7))*1000))
                               .build());
                   }
               }
                       }
                    }

        }











