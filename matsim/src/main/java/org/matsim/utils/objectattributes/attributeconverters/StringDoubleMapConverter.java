package org.matsim.utils.objectattributes.attributeconverters;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.utils.objectattributes.AttributeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.matsim.utils.objectattributes.StringDoubleMap;

public class StringDoubleMapConverter implements AttributeConverter<StringDoubleMap> {
    private static final Logger LOG = LogManager.getLogger(StringDoubleMapConverter.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MapType mapType = TypeFactory.defaultInstance().constructMapType(Map.class, String.class,
            Double.class);

    @Override
    public StringDoubleMap convert(String value) {
        try {
            return new StringDoubleMap(mapper.readValue(value, mapType));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String convertToString(Object o) {
        if (!(o instanceof StringDoubleMap)) {
            LOG.error("Object is not of type StringDoubleMap: {}", o.getClass());
            return null;
        }

        return new StringStringMapConverter().convertToString(o);
    }
}
