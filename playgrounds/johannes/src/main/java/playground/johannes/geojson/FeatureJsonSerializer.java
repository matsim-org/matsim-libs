package playground.johannes.geojson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

import java.lang.reflect.Type;

public class FeatureJsonSerializer implements JsonSerializer<Feature> {
    public JsonElement serialize(Feature feature, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "Feature");
        json.add("geometry", jsonSerializationContext.serialize(feature.getDefaultGeometryProperty().getValue()));
        JsonObject properties = new JsonObject();
        for (Property property : feature.getProperties()) {
            String name = property.getName().getURI();
            Object value = property.getValue();
            if (feature.getType().getGeometryDescriptor().getName().getURI().equals(name)) {
                continue;
            }
            properties.add(name, jsonSerializationContext.serialize(value, property.getType().getBinding()));
        }
        json.add("properties", properties);
        if (feature.getType().isIdentified()) {
            json.addProperty("id", feature.getIdentifier().getID());
        }
        return json;
    }
}
