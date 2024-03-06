package org.matsim.utils.objectattributes.attributeconverters;

import java.util.LinkedHashMap;
import java.util.Map;

public class StringDoubleMap extends LinkedHashMap<String, Double> {
    public StringDoubleMap() {
        super();
    }

   public StringDoubleMap(Map<String,Double> map) {
        super(map);
    }
}
