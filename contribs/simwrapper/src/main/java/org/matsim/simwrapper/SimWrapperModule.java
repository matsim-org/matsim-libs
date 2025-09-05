package org.matsim.simwrapper;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Install the SimWrapper Extension into MATSim.
 */
public final class SimWrapperModule extends AbstractModule {
	private static final Logger log = LoggerFactory.getLogger(SimWrapperModule.class);

	private final SimWrapper simWrapper;

	/**
	 * Create module with existing simwrapper instance.
	 */
	public SimWrapperModule(SimWrapper simWrapper) {
		this.simWrapper = simWrapper;
	}

	/**
	 * Constructor with a newly initialized {@link SimWrapper} instance.
	 */
	public SimWrapperModule() {
		this.simWrapper = null;
	}

	@Override
	public void install() {

		addControllerListenerBinding().to(SimWrapperListener.class);

		// Construct the binder one time, even through nothing is added
		// otherwise the injection will not work
		Multibinder.newSetBinder(binder(), Dashboard.class);
		Multibinder.newSetBinder(binder(), DashboardProvider.class);

		if (ConfigUtils.addOrGetModule(getConfig(), SimWrapperConfigGroup.class).getDefaultDashboards() != SimWrapperConfigGroup.Mode.disabled) {
			SimWrapper.addDashboardProviderBinding(binder()).toInstance(new DefaultDashboardProvider());
		}

		loadFromOtherSources();
	}

	/**
	 * This function is mainly there for backwards compatibility.
	 */
	private void loadFromOtherSources() {
		// Load provider from packages
		for (String pack : ConfigUtils.addOrGetModule(getConfig(), SimWrapperConfigGroup.class).getPackages()) {

			log.info("Scanning package {}", pack);

			try {
				ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses(pack);

				List<DashboardProvider> providers = loadProvider(classes);

				for (DashboardProvider provider : providers) {
					SimWrapper.addDashboardProviderBinding(binder()).toInstance(provider);
				}

			} catch (IOException e) {
				log.error("Could not add providers from package {}", pack, e);
			}
		}

		// Load from Spi
		if (ConfigUtils.addOrGetModule(getConfig(), SimWrapperConfigGroup.class).getLoading() == SimWrapperConfigGroup.DashboardLoading.spiAndGuice) {
			ServiceLoader<DashboardProvider> loader = ServiceLoader.load(DashboardProvider.class);
			for (DashboardProvider dashboardProvider : loader) {
				SimWrapper.addDashboardProviderBinding(binder()).toInstance(dashboardProvider);
			}
		}
	}

	@Provides
	@Singleton
	public SimWrapper getSimWrapper(Config config) {
		if (simWrapper == null)
			return SimWrapper.create(config);

		return simWrapper;
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
}
