package pharma;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

import java.io.File;
import java.io.IOException;

/**
 * Created by schroeder on 30/10/15.
 */
public class Pharmacies {

    public static void main(String[] args) throws IOException {

        ActivityFacilities facts = FacilitiesUtils.createActivityFacilities("facilities");

        GeotoolsTransformation transformation = new GeotoolsTransformation(TransformationFactory.WGS84,TransformationFactory.DHDN_GK4);


        ObjectMapper mapper = new ObjectMapper();
        JsonParser jParser = mapper.getFactory().createParser(new File("/Users/schroeder/DLR/Pharma/data/pharmacies.geojson"));
        JsonNode root = mapper.readTree(jParser);

        JsonNode features = root.findPath("features");
        int facilityId = 1;
        for(JsonNode feature : features){
            JsonNode coordinates = feature.findPath("coordinates");
            if(coordinates.isArray()){
                if(coordinates.get(0).isArray()){
                    double x = coordinates.get(0).get(0).get(0).asDouble();
                    double y = coordinates.get(0).get(0).get(1).asDouble();
                    Coord coord = new Coord(x,y);
                    ActivityFacility fac = facts.getFactory().createActivityFacility(Id.create(facilityId, ActivityFacility.class),transformation.transform(coord));
                    facts.addActivityFacility(fac);
                    facilityId++;
                }
                else {
                    double x = coordinates.get(0).asDouble();
                    double y = coordinates.get(1).asDouble();
                    Coord coord = new Coord(x,y);
                    ActivityFacility fac = facts.getFactory().createActivityFacility(Id.create(facilityId, ActivityFacility.class),transformation.transform(coord));
                    facts.addActivityFacility(fac);
                    facilityId++;
//                    System.out.println(coordinates);
                }

            }
        }

        new FacilitiesWriter(facts).write("output/pharmacies.xml");

    }
}
