package pharma;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by schroeder on 30/10/15.
 */
public class Pharmacies {

    public static void main(String[] args) throws IOException {

        ActivityFacilities facts = FacilitiesUtils.createActivityFacilities("facilities");

        ObjectMapper mapper = new ObjectMapper();
        JsonParser jParser = mapper.getFactory().createParser(new File("/Users/schroeder/DLR/Pharma/data/pharmacies.geojson"));
        JsonNode root = mapper.readTree(jParser);

        JsonNode features = root.findPath("features");
        for(JsonNode feature : features){
            JsonNode coordinates = feature.findPath("coordinates");
            if(coordinates.isArray()){
                if(coordinates.get(0).isArray()){
                    System.out.println(coordinates.get(0).get(0));
                }
                else {
                    System.out.println(coordinates);
                }

            }
        }

    }
}
