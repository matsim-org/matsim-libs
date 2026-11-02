package org.matsim.contrib.msp.capability.facilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the places at which discretionary activities can be performed out of a standard MATSim
 * {@link ActivityFacilities} container. No format of its own: a facilities file as MATSim reads and
 * writes it carries everything, it only has to be annotated.
 * <p>
 * <b>Type.</b> Taken from the facility attribute named by
 * {@link CapabilityConfigGroup#getFacilityTypeAttribute()} (default {@code capabilityType}), which may
 * list several types separated by commas. A facility without that attribute falls back to the types of
 * its {@code <activity>} options. A facility with neither is skipped — nothing can be done there.
 * <p>
 * <b>Sub-type.</b> Taken from the facility attribute named by
 * {@link CapabilityConfigGroup#getFacilitySubTypeAttribute()} (default {@code capabilitySubType}),
 * and simply absent if the facility does not carry it.
 * <p>
 * <b>Opening hours.</b> Taken from the {@code <opentime>} elements of the activity option whose type
 * equals the capability type. If the facility has no such option, the
 * {@link #OPENING_TIME_ATTRIBUTE} and {@link #CLOSING_TIME_ATTRIBUTE} attributes are used instead.
 * If it carries neither, the place counts as open around the clock, which is the MATSim convention
 * for an activity option without opening times.
 * <p>
 * So a facility of a scenario that was never annotated still yields usable entries — one per activity
 * option, with that option's opening hours — and annotating it with {@code capabilityType} and
 * {@code capabilitySubType} is what adds the capability vocabulary on top.
 *
 * <pre>{@code
 * <facility id="super-7" x="4621000" y="5820000" linkId="123">
 *   <attributes>
 *     <attribute name="capabilityType" class="java.lang.String">shop</attribute>
 *     <attribute name="capabilitySubType" class="java.lang.String">Konsum</attribute>
 *   </attributes>
 *   <activity type="shop">
 *     <opentime day="wkday" start_time="08:00:00" end_time="20:00:00"/>
 *   </activity>
 * </facility>
 * }</pre>
 */
public final class DiscretionaryActivityFacilityExtractor {

	/**
	 * Facility attribute the opening time is read from if the facility has no matching activity option.
	 * Either a time string such as {@code 08:00:00} or a number of seconds.
	 */
	public static final String OPENING_TIME_ATTRIBUTE = "openingTime";

	/**
	 * Facility attribute the closing time is read from if the facility has no matching activity option.
	 *
	 * @see #OPENING_TIME_ATTRIBUTE
	 */
	public static final String CLOSING_TIME_ATTRIBUTE = "closingTime";

	private static final Logger log = LogManager.getLogger(DiscretionaryActivityFacilityExtractor.class);

	private final CapabilityConfigGroup config;

	public DiscretionaryActivityFacilityExtractor(CapabilityConfigGroup config) {
		this.config = config;
	}

	/**
	 * @return the places at which discretionary activities can be performed, one entry per facility and
	 * type. Facilities that do not say what can be done at them are skipped.
	 */
	public DiscretionaryActivityFacilities extract(ActivityFacilities facilities) {

		List<DiscretionaryActivityFacility> extracted = new ArrayList<>();
		int withoutType = 0;

		for (ActivityFacility facility : facilities.getFacilities().values()) {

			Set<String> types = typesOf(facility);
			if (types.isEmpty()) {
				withoutType++;
				continue;
			}

			String subType = subTypeOf(facility);
			for (String type : types) {
				extracted.add(new DiscretionaryActivityFacility(facility, type, subType, openingTimesOf(facility, type)));
			}
		}

		DiscretionaryActivityFacilities result = new DiscretionaryActivityFacilities(extracted);

		if (withoutType > 0) {
			log.warn("{} of {} facilities say neither in the '{}' attribute nor in an activity option what can be "
				+ "done at them and are skipped.", withoutType, facilities.getFacilities().size(),
				this.config.getFacilityTypeAttribute());
		}

		log.info("Found {} places for discretionary activities: {}", result.size(), result);

		return result;
	}

	/**
	 * @return what can be done at the given facility, empty if it does not say
	 */
	private Set<String> typesOf(ActivityFacility facility) {

		String attribute = stringAttribute(facility, this.config.getFacilityTypeAttribute());

		if (attribute != null) {
			Set<String> types = new LinkedHashSet<>();
			for (String type : attribute.split(",")) {
				if (!type.isBlank()) {
					types.add(type.strip());
				}
			}
			return types;
		}

		// not annotated: fall back to what the scenario itself says can be done here
		return new LinkedHashSet<>(facility.getActivityOptions().keySet());
	}

	private String subTypeOf(ActivityFacility facility) {
		return stringAttribute(facility, this.config.getFacilitySubTypeAttribute());
	}

	/**
	 * @return when the facility is open for the given type, empty if it is always open
	 */
	private Collection<OpeningTime> openingTimesOf(ActivityFacility facility, String type) {

		ActivityOption option = facility.getActivityOptions().get(type);
		if (option != null) {
			return option.getOpeningTimes();
		}

		// a user defined type the scenario has no activity option for: the facility has to say when it
		// is open in its attributes, or it counts as always open
		double opening = timeAttribute(facility, OPENING_TIME_ATTRIBUTE, Double.NEGATIVE_INFINITY);
		double closing = timeAttribute(facility, CLOSING_TIME_ATTRIBUTE, Double.POSITIVE_INFINITY);

		if (opening == Double.NEGATIVE_INFINITY && closing == Double.POSITIVE_INFINITY) {
			return List.of();
		}

		if (closing <= opening) {
			log.warn("Facility {} closes at {} before it opens at {}, treating it as always open.",
				facility.getId(), Time.writeTime(closing), Time.writeTime(opening));
			return List.of();
		}

		return List.of(new OpeningTimeImpl(opening, closing));
	}

	private static String stringAttribute(ActivityFacility facility, String name) {
		Object value = facility.getAttributes().getAttribute(name);
		if (value == null) {
			return null;
		}
		String text = value.toString().strip();
		return text.isEmpty() ? null : text;
	}

	/**
	 * @return the named time attribute, either as a number of seconds or as a time string such as
	 * {@code 08:00:00}, or the given fallback if the facility does not carry it
	 */
	private static double timeAttribute(ActivityFacility facility, String name, double fallback) {

		Object value = facility.getAttributes().getAttribute(name);

		if (value == null) {
			return fallback;
		}
		if (value instanceof Number number) {
			return number.doubleValue();
		}

		String text = value.toString().strip();
		try {
			return Time.parseTime(text);
		} catch (NumberFormatException e) {
			log.warn("Facility {} has an unreadable '{}' attribute '{}', ignoring it.", facility.getId(), name, text);
			return fallback;
		}
	}

	/**
	 * Convenience for the common case of annotating a facility by hand or in a test.
	 *
	 * @see #OPENING_TIME_ATTRIBUTE
	 */
	public static void annotate(ActivityFacility facility, CapabilityConfigGroup config, String subType, String... types) {
		facility.getAttributes().putAttribute(config.getFacilityTypeAttribute(), String.join(",", Arrays.asList(types)));
		if (subType != null) {
			facility.getAttributes().putAttribute(config.getFacilitySubTypeAttribute(), subType);
		}
	}
}
