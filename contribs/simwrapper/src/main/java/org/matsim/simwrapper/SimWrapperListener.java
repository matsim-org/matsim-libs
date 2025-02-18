package org.matsim.simwrapper;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Listener to execute {@link SimWrapper} when simulation starts or ends.
 */
public class SimWrapperListener implements StartupListener, ShutdownListener {

	private static final Logger log = LogManager.getLogger(SimWrapper.class);
	/**
	 * Run priority of SimWrapper. Generally, it should run after alls other listeners.
	 */
	public static double PRIORITY = -1000;
	private final SimWrapper simWrapper;
	private final Set<Dashboard> bindings;
	private final Config config;

	@Inject
	public SimWrapperListener(SimWrapper simWrapper, Set<Dashboard> bindings, Config config) {
		this.simWrapper = simWrapper;
		this.bindings = bindings;
		this.config = config;
	}

	/**
	 * Create a new listener with no default bindings.
	 */
	public SimWrapperListener(SimWrapper simWrapper, Config config) {
		this(simWrapper, Collections.emptySet(), config);
	}

	@Override
	public double priority() {
		return PRIORITY;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		generate(Path.of(event.getServices().getControlerIO().getOutputPath()));
	}


	private void generate(Path output) {
		SimWrapperConfigGroup config = simWrapper.getConfigGroup();

		// Load provider from packages
		for (String pack : config.packages) {

			log.info("Scanning package {}", pack);

			try {
				ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses(pack);

				List<DashboardProvider> providers = loadProvider(classes);
				addFromProvider(config, providers);

			} catch (IOException e) {
				log.error("Could not add providers from package {}", pack, e);
			}
		}

		// Lambda provider which uses dashboards from bindings
		addFromProvider(config, List.of((c, sw) -> new ArrayList<>(bindings)));

		// Dashboard provider services
		if (config.defaultDashboards != SimWrapperConfigGroup.Mode.disabled) {
			ServiceLoader<DashboardProvider> loader = ServiceLoader.load(DashboardProvider.class);
			addFromProvider(config, loader);
		}

		try {
			simWrapper.generate(output);
		} catch (IOException e) {
			log.error("Could not create SimWrapper dashboard.");
		}
	}

	private void addFromProvider(SimWrapperConfigGroup config, Iterable<DashboardProvider> providers) {

		List<DashboardProvider> list = StreamSupport.stream(providers.spliterator(), false)
			.sorted(Comparator.comparingDouble(DashboardProvider::priority).reversed())
			.toList();

		for (DashboardProvider provider : list) {
			log.info("Creating dashboards for {}", provider);

			for (Dashboard d : provider.getDashboards(this.config, this.simWrapper)) {

				if (config.exclude.contains(d.getClass().getSimpleName()) || config.exclude.contains(d.getClass().getName()))
					continue;

				if (!config.include.isEmpty() && (!config.include.contains(d.getClass().getSimpleName()) && !config.include.contains(d.getClass().getName())))
					continue;

				if (!simWrapper.hasDashboard(d.getClass(), d.context()) || d instanceof Dashboard.Customizable) {
					log.info("Adding dashboard {}", d);
					simWrapper.addDashboard(d);
				} else
					log.warn("Skipping dashboard {} with context {}, because it is already present", d, d.context());
			}
		}
	}

	private List<DashboardProvider> loadProvider(ImmutableSet<ClassPath.ClassInfo> classes) {
		List<DashboardProvider> result = new ArrayList<>();
		for (ClassPath.ClassInfo info : classes) {
			Class<?> clazz = info.load();
			if (DashboardProvider.class.isAssignableFrom(clazz)) {
				try {
					Constructor<?> c = clazz.getDeclaredConstructor();
					DashboardProvider o = (DashboardProvider) c.newInstance();
					result.add(o);
				} catch (ReflectiveOperationException e) {
					log.error("Could not create provider {}", info, e);
				}
			}
		}
		return result;
	}

	/**
	 * This needs to run after all the other MATSim listeners. Currently, this is the case, but unclear if that is ensured somewhere.
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		simWrapper.run(Path.of(event.getServices().getControlerIO().getOutputPath()));
	}

	/**
	 * Run dashboard creation and execution. This method is useful when used outside MATSim.
	 */
	public void run(Path output) throws IOException {
		run(output, null);
	}

	void run(Path output, @Nullable String configPath) throws IOException {
		generate(output);
		simWrapper.run(output, configPath);
	}

}
