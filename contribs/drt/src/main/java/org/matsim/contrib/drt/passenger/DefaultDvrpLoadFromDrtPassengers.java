package org.matsim.contrib.drt.passenger;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpLoadSerializer;
import org.matsim.contrib.dvrp.fleet.DvrpLoadType;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;

public class DefaultDvrpLoadFromDrtPassengers implements DvrpLoadFromDrtPassengers {

	public static final String ATTRIBUTE_LOAD_TYPE = "drt:loadType";
	public static final String ATTRIBUTE_LOAD_VALUE = "drt:loadValue";

	private final Population population;
	private final DvrpLoadSerializer dvrpLoadSerializer;
	private final IntegerLoadType fallBackIntegerLoadType;

	@Inject
	public DefaultDvrpLoadFromDrtPassengers(Population population, DvrpLoadSerializer dvrpLoadSerializer, IntegerLoadType fallBackIntegerLoadType) {
		this.population = population;
		this.dvrpLoadSerializer = dvrpLoadSerializer;
		this.fallBackIntegerLoadType = fallBackIntegerLoadType;
	}

	public DvrpLoad getPersonLoad(Id<Person> personId) {
		Person person = Objects.requireNonNull(this.population.getPersons().get(personId), () -> String.format("Person %s not found", personId.toString()));
		DvrpLoad dvrpLoad;
		String loadTypeAttributeString  = (String) person.getAttributes().getAttribute(ATTRIBUTE_LOAD_TYPE);
		String loadValueAttributeString = (String) person.getAttributes().getAttribute(ATTRIBUTE_LOAD_VALUE);
		if(loadTypeAttributeString != null) {
			Id<DvrpLoadType> dvrpLoadTypeId = Id.create(loadTypeAttributeString, DvrpLoadType.class);
			dvrpLoad = dvrpLoadSerializer.deSerialize(Objects.requireNonNullElse(loadValueAttributeString, "1"), dvrpLoadTypeId);
		} else {
			int loadValueInteger;
			if(loadValueAttributeString != null) {
				try {
					loadValueInteger = Integer.parseInt(loadValueAttributeString);
				} catch(NumberFormatException e) {
					throw new IllegalStateException(String.format("%s attribute must be an integer when no %s is used", ATTRIBUTE_LOAD_VALUE, ATTRIBUTE_LOAD_TYPE));
				}
			} else {
				loadValueInteger = 1;
			}
			dvrpLoad = this.fallBackIntegerLoadType.fromInt(loadValueInteger);
		}
		return dvrpLoad;
	}

	@Override
	@SuppressWarnings("")
	public DvrpLoad getLoad(Collection<Id<Person>> personIds) {
		try {
			return personIds.stream().map(this::getPersonLoad).reduce(DvrpLoad::addTo).orElseThrow();
		} catch (NoSuchElementException e) {
			throw new IllegalStateException("At least one person should be behind a request", e);
		} catch (DvrpLoad.UnsupportedVehicleLoadException e) {
			throw new IllegalStateException("Persons with incompatible loads behind the same request", e);
		}
	}
}
