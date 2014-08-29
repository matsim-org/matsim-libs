package playground.johannes.geojson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;

public class GeoJson {
    private final Gson gson;

    public GeoJson() {
        gson = new GsonBuilder()
                //.serializeNulls()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Feature.class, new FeatureJsonSerializer())
                .registerTypeHierarchyAdapter(FeatureCollection.class, new FeatureCollectionJsonSerializer())
                .create();
    }
    
    public String toJson(Object src) {
        return gson.toJson(src);
    }

    public void toJson(Object src, Appendable writer) {
        gson.toJson(src, writer);
    }
}
