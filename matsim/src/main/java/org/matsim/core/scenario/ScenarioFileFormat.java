package org.matsim.core.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * SPI for alternative scenario file formats (e.g. protobuf, parquet).
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * <p>
 * To register a provider, add a file
 * {@code META-INF/services/org.matsim.core.scenario.ScenarioFileFormat}
 * containing the fully qualified class name of the implementation.
 * <p>
 * A provider declares which file extensions it handles. When {@link ScenarioLoaderImpl}
 * encounters a file with a matching extension, it delegates to the provider instead of
 * using the built-in XML readers.
 * <p>
 * All methods have default implementations that throw {@link UnsupportedOperationException},
 * so providers only need to implement the methods for the types they support.
 *
 * @author nkuehnel / MOIA
 */
public interface ScenarioFileFormat {

	/**
	 * @return file extensions this provider handles (without leading dot), e.g. "pb", "pbf", "parquet"
	 */
	Set<String> getSupportedExtensions();

	default void readPopulation(URL url, Scenario scenario, String inputCRS, String targetCRS,
								Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		throw new UnsupportedOperationException("Population reading not supported by " + getClass().getName());
	}

	default void writePopulation(Population population, String filename,
								 Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		throw new UnsupportedOperationException("Population writing not supported by " + getClass().getName());
	}

	default void readNetwork(URL url, Scenario scenario, String inputCRS, String targetCRS,
							 Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		throw new UnsupportedOperationException("Network reading not supported by " + getClass().getName());
	}

	default void readFacilities(URL url, Scenario scenario, String inputCRS, String targetCRS,
								Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		throw new UnsupportedOperationException("Facilities reading not supported by " + getClass().getName());
	}

	default void readVehicles(URL url, Scenario scenario) {
		throw new UnsupportedOperationException("Vehicles reading not supported by " + getClass().getName());
	}

	default void readTransitSchedule(URL url, Scenario scenario, String inputCRS, String targetCRS) {
		throw new UnsupportedOperationException("Transit schedule reading not supported by " + getClass().getName());
	}

	default void readTransitVehicles(URL url, Scenario scenario) {
		throw new UnsupportedOperationException("Transit vehicles reading not supported by " + getClass().getName());
	}

	default void readHouseholds(URL url, Scenario scenario, Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		throw new UnsupportedOperationException("Households reading not supported by " + getClass().getName());
	}
}