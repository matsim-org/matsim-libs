package playground.johannes.geojson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import java.lang.reflect.Type;

public class FeatureCollectionJsonSerializer implements JsonSerializer<FeatureCollection> {
    public JsonElement serialize(FeatureCollection featureCollection, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "FeatureCollection");
        JsonArray features = new JsonArray();
        FeatureIterator i = featureCollection.features();
        try {
            while (i.hasNext()) {
                Feature feature = i.next();
                features.add(jsonSerializationContext.serialize(feature));
            }
        } finally {
            i.close();
        }
        json.add("features", features);
        return json;
    }
}
