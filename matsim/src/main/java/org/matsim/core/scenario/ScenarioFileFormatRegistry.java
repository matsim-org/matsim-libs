package org.matsim.core.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Discovers {@link ScenarioFileFormat} implementations via {@link ServiceLoader}
 * and resolves the appropriate provider for a given filename.
 *
 * @author nkuehnel / MOIA
 */
public final class ScenarioFileFormatRegistry {

	private static final Logger log = LogManager.getLogger(ScenarioFileFormatRegistry.class);

	private static final Map<String, ScenarioFileFormat> providers = new HashMap<>();

	static {
		for (ScenarioFileFormat provider : ServiceLoader.load(ScenarioFileFormat.class)) {
			for (String ext : provider.getSupportedExtensions()) {
				providers.put(ext, provider);
				log.info("Registered ScenarioFileFormat provider for extension '." + ext + "': " + provider.getClass().getName());
			}
		}
	}

	private ScenarioFileFormatRegistry() {
	}

	/**
	 * Find a provider for the given filename based on its extension.
	 * Handles double extensions like "population.pb.zst" by stripping compression suffixes first.
	 */
	public static Optional<ScenarioFileFormat> getProvider(String filename) {
		return getEffectiveExtension(filename).map(providers::get);
	}

	static Optional<String> getEffectiveExtension(String filename) {
		int lastDot = filename.lastIndexOf('.');
		if (lastDot < 0) {
			return Optional.empty();
		}
		String ext = filename.substring(lastDot + 1);
		if (ext.equals("zst") || ext.equals("gz") || ext.equals("bz2")) {
			String inner = filename.substring(0, lastDot);
			int innerDot = inner.lastIndexOf('.');
			if (innerDot >= 0) {
				ext = inner.substring(innerDot + 1);
			}
		}
		return Optional.of(ext);
	}
}