package org.matsim.utils.objectattributes.attributeconverters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.vehicles.PersonVehicles;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class PersonVehiclesAttributeConverter implements AttributeConverter<PersonVehicles> {

    private final Logger logger = Logger.getLogger(PersonVehiclesAttributeConverter.class);

    @Override
    public PersonVehicles convert(String value) {
        PersonVehicles vehicles = new PersonVehicles();
        Map<String, String> stringMap = new StringStringMapConverter().convert(value);
        for (Map.Entry<String, String> entry: stringMap.entrySet()) {
            vehicles.addModeVehicle(entry.getKey(), Id.createVehicleId(entry.getValue()));
        }
        return vehicles;
    }

    @Override
    public String convertToString(Object o) {
        if(!(o instanceof PersonVehicles)){
            logger.error("Object is not of type PersonVehicles: " + o.getClass().toString());
            return null;
        }
        PersonVehicles vehicles = (PersonVehicles)o;
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, Id<Vehicle>> entry: vehicles.getModeVehicles().entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue().toString());
        }
        return new StringStringMapConverter().convertToString(stringMap);
    }
}
