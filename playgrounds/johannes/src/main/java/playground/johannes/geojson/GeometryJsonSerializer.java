package playground.johannes.geojson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.lang.reflect.Type;

public class GeometryJsonSerializer implements JsonSerializer<Geometry> {
    public JsonElement serialize(Geometry geometry, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject json = new JsonObject();
        json.addProperty("type", geometry.getGeometryType());
        String geometryType = geometry.getGeometryType();
        if (geometryType.equals("Point")) {
            JsonArray coordinates = pointCoordinates((Point) geometry);
            json.add("coordinates", coordinates);
        } else if (geometryType.equals("MultiPoint")) {
            JsonArray coordinates = new JsonArray();
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Point child = (Point) geometry.getGeometryN(i);
                coordinates.add(pointCoordinates(child));
            }
            json.add("coordinates", coordinates);
        } else if (geometryType.equals("LineString")) {
            JsonArray coordinates = lineStringCoordinates((LineString) geometry);
            json.add("coordinates", coordinates);
        } else if (geometryType.equals("MultiLineString")) {
            JsonArray coordinates = new JsonArray();
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                LineString child = (LineString) geometry.getGeometryN(i);
                coordinates.add(lineStringCoordinates(child));
            }
            json.add("coordinates", coordinates);
        } else if (geometryType.equals("Polygon")) {
            JsonArray coordinates = polygonCoordinates((Polygon) geometry);
            json.add("coordinates", coordinates);
        } else if (geometryType.equals("MultiPolygon")) {
            JsonArray coordinates = new JsonArray();
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Polygon child = (Polygon) geometry.getGeometryN(i);
                coordinates.add(polygonCoordinates(child));
            }
            json.add("coordinates", coordinates);
        } else if (geometryType.equals("GeometryCollection")) {
            JsonArray geometries = new JsonArray();
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Geometry child = geometry.getGeometryN(i);
                geometries.add(jsonSerializationContext.serialize(child));
            }
            json.add("geometries", geometries);
        } else {
            throw new IllegalArgumentException("Unknown geometry type " + geometry.getGeometryType());
        }
        return json;
    }

    JsonArray pointCoordinates(Point geometry) {
        return toJson(geometry.getCoordinate());
    }

    JsonArray lineStringCoordinates(LineString geometry) {
        return toJson(geometry.getCoordinates());
    }

    JsonArray polygonCoordinates(Polygon polygon) {
        JsonArray result = new JsonArray();
        result.add(toJson(polygon.getExteriorRing().getCoordinates()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            result.add(toJson(polygon.getInteriorRingN(i).getCoordinates()));
        }
        return result;
    }

    JsonArray toJson(Coordinate[] coordinates) {
        JsonArray result = new JsonArray();
        for (Coordinate coordinate : coordinates) {
            result.add(toJson(coordinate));
        }
        return result;
    }

    JsonArray toJson(Coordinate coordinate) {
        JsonArray result = new JsonArray();
        result.add(new JsonPrimitive(coordinate.x));
        result.add(new JsonPrimitive(coordinate.y));
        if (!Double.isNaN(coordinate.z)) {
            result.add(new JsonPrimitive(coordinate.z));
        }
        return result;
    }
}
