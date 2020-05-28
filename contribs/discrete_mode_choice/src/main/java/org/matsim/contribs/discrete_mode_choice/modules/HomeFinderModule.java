package org.matsim.contribs.discrete_mode_choice.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.ActivityTypeHomeFinder;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.FirstActivityHomeFinder;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityHomeFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Internal module that manages all built-in HomeFinder implementations.
 * 
 * @author sebhoerl
 *
 */
public class HomeFinderModule extends AbstractDiscreteModeChoiceExtension {
	public static final String FIRST_ACTIVITY = "FirstActivity";
	public static final String ACTIVITY_BASED = "ActivityBased";

	public static final Collection<String> COMPONENTS = Arrays.asList(FIRST_ACTIVITY, ACTIVITY_BASED);

	@Override
	public void installExtension() {
		bindHomeFinder(FIRST_ACTIVITY).to(FirstActivityHomeFinder.class);
		bindHomeFinder(ACTIVITY_BASED).to(ActivityTypeHomeFinder.class);
	}

	@Provides
	@Singleton
	public FirstActivityHomeFinder provideFirstActivityHomeFinder() {
		return new FirstActivityHomeFinder();
	}

	@Provides
	@Singleton
	public ActivityTypeHomeFinder provideActivityTypeHomeFinder(DiscreteModeChoiceConfigGroup dmcConfig) {
		ActivityHomeFinderConfigGroup config = dmcConfig.getActivityHomeFinderConfigGroup();
		return new ActivityTypeHomeFinder(config.getActivityTypes());
	}

	@Provides
	@Singleton
	public HomeFinder provideHomeFinder(DiscreteModeChoiceConfigGroup dmcConfig,
			Map<String, Provider<HomeFinder>> components) {
		Provider<HomeFinder> provider = components.get(dmcConfig.getHomeFinder());

		if (provider != null) {
			return provider.get();
		} else {
			throw new IllegalStateException(
					String.format("There is no HomeFinder component called '%s',", dmcConfig.getHomeFinder()));
		}
	}
}
