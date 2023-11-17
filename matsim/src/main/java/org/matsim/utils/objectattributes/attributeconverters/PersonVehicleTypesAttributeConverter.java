package org.matsim.utils.objectattributes.attributeconverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.vehicles.PersonVehicleTypes;
import org.matsim.vehicles.VehicleType;

import java.util.HashMap;
import java.util.Map;

/**
 * Converter to store vehicle types as person attribute.
 */
public class PersonVehicleTypesAttributeConverter implements AttributeConverter<PersonVehicleTypes> {

    private final Logger logger = LogManager.getLogger(PersonVehicleTypesAttributeConverter.class);

    @Override
    public PersonVehicleTypes convert(String value) {
        PersonVehicleTypes vehicles = new PersonVehicleTypes();
        Map<String, String> stringMap = new StringStringMapConverter().convert(value);
        for (Map.Entry<String, String> entry: stringMap.entrySet()) {
            vehicles.addModeVehicleType(entry.getKey(), Id.create(entry.getValue(), VehicleType.class));
        }
        return vehicles;
    }

    @Override
    public String convertToString(Object o) {
        if(!(o instanceof PersonVehicleTypes vehicles)){
            logger.error("Object is not of type PersonVehicles: " + o.getClass());
            return null;
        }
		Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, Id<VehicleType>> entry: vehicles.getModeVehicleTypes().entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue().toString());
        }
        return new StringStringMapConverter().convertToString(stringMap);
    }
}
