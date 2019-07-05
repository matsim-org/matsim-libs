package vwExamples.utils.CreateShiftingScenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class ShiftingScenario {
	Map<String, Double> modes2ShiftratesMap;
	Map<String,MutableInt> mode2TripCounter = new HashMap<String,MutableInt>();
	Set<Id<Person>> agentSet = new HashSet<Id<Person>>();
	Double subTourConversionRate;
	MutableInt totalSubtourCounter = new MutableInt(0);
	String type;

	ShiftingScenario(Double subTourConversionRate) {
		this.subTourConversionRate = subTourConversionRate;
		this.type = "subtourConversion";
	}
	
	ShiftingScenario(Map<String, Double> modes2ShiftratesMap) {
		this.modes2ShiftratesMap = modes2ShiftratesMap;
		this.type = "tripConversion";
	}

}
