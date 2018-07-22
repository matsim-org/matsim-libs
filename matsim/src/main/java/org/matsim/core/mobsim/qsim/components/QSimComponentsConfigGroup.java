package org.matsim.core.mobsim.qsim.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.config.ReflectiveConfigGroup.StringSetter;

public class QSimComponentsConfigGroup extends ConfigGroup {
	public static final String GROUP_NAME = "qsim_components";

	public static final String ACTIVE_MOBSIM_ENGINES = "activeMobsimEngines";
	public static final String ACTIVE_ACTIVITY_HANDLERS = "activeActivityHandlers";
	public static final String ACTIVE_DEPATURE_HANDLERS = "activeDepartureHandlers";
	public static final String ACTIVE_AGENT_SOURCES = "activeAgentSources";

	public final static List<String> DEFAULT_MOBSIM_ENGINES = Arrays.asList("ActivityEngine", "NetsimEngine",
			"TeleportationEngine");

	public final static List<String> DEFAULT_ACTIVITY_HANDLERS = Arrays.asList("ActivityEngine");

	public final static List<String> DEFAULT_DEPARTURE_HANDLERS = Arrays.asList("NetsimEngine" // ,
																								// "TeleportationEngine"
	);

	public final static List<String> DEFAULT_AGENT_SOURCES = Arrays.asList("PopulationAgentSource");

	private List<String> activeMobsimEngines = new LinkedList<>(DEFAULT_MOBSIM_ENGINES);
	private List<String> activeActivityHandlers = new LinkedList<>(DEFAULT_ACTIVITY_HANDLERS);
	private List<String> activeDepartureHandlers = new LinkedList<>(DEFAULT_DEPARTURE_HANDLERS);
	private List<String> activeAgentSources = new LinkedList<>(DEFAULT_AGENT_SOURCES);

	public QSimComponentsConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = new HashMap<>();

		map.put(ACTIVE_MOBSIM_ENGINES,
				"Defines which MobsimEngines are active and in which order they are registered. Depending on which extensions and contribs you use, it may be necessary to define additional components here. Default is: "
						+ String.join(", ", DEFAULT_MOBSIM_ENGINES));
		map.put(ACTIVE_ACTIVITY_HANDLERS,
				"Defines which ActivityHandlers are active and in which order they are registered. Depending on which extensions and contribs you use, it may be necessary to define additional components here.  Default is: "
						+ String.join(", ", DEFAULT_ACTIVITY_HANDLERS));
		map.put(ACTIVE_DEPATURE_HANDLERS,
				"Defines which DepartureHandlers are active and in which order they are registered. Depending on which extensions and contribs you use, it may be necessary to define additional components here.  Default is: "
						+ String.join(", ", DEFAULT_DEPARTURE_HANDLERS));
		map.put(ACTIVE_AGENT_SOURCES,
				"Defines which AgentSources are active and in which order they are registered. Depending on which extensions and contribs you use, it may be necessary to define additional components here.  Default is: "
						+ String.join(", ", DEFAULT_AGENT_SOURCES));

		return map;
	}

	public List<String> getActiveMobsimEngines() {
		return activeMobsimEngines;
	}

	public void setActiveMobsimEngines(List<String> activeMobsimEngines) {
		this.activeMobsimEngines = activeMobsimEngines;
	}

	@StringGetter(ACTIVE_MOBSIM_ENGINES)
	public String getActiveMobsimEnginesAsString() {
		return String.join(", ", activeMobsimEngines);
	}

	@StringSetter(ACTIVE_MOBSIM_ENGINES)
	public void setActiveMobsimEnginesAsString(String activeMobsimEngines) {
		this.activeMobsimEngines = interpretQSimComponents(this.activeMobsimEngines, activeMobsimEngines);
	}

	public List<String> getActiveActivityHandlers() {
		return activeActivityHandlers;
	}

	public void setActiveActivityHandlers(List<String> activeActivityHandlers) {
		this.activeActivityHandlers = activeActivityHandlers;
	}

	@StringGetter(ACTIVE_ACTIVITY_HANDLERS)
	public String getActiveActivityHandlersAsString() {
		return String.join(", ", activeActivityHandlers);
	}

	@StringSetter(ACTIVE_ACTIVITY_HANDLERS)
	public void setActiveActivityHandlersAsString(String activeActivityHandlers) {
		this.activeActivityHandlers = interpretQSimComponents(this.activeActivityHandlers, activeActivityHandlers);
	}

	public List<String> getActiveDepartureHandlers() {
		return activeDepartureHandlers;
	}

	public void setActiveDepartureHandlers(List<String> activeDepartureHandlers) {
		this.activeDepartureHandlers = activeDepartureHandlers;
	}

	@StringGetter(ACTIVE_DEPATURE_HANDLERS)
	public String getActiveDepartureHandlersAsString() {
		return String.join(", ", activeDepartureHandlers);
	}

	@StringSetter(ACTIVE_DEPATURE_HANDLERS)
	public void setActiveDepartureHandlersAsString(String activeDepartureHandlers) {
		this.activeDepartureHandlers = interpretQSimComponents(this.activeDepartureHandlers, activeDepartureHandlers);
	}

	public List<String> getActiveAgentSources() {
		return activeAgentSources;
	}

	public void setActiveAgentSources(List<String> activeAgentSources) {
		this.activeAgentSources = activeAgentSources;
	}

	@StringGetter(ACTIVE_AGENT_SOURCES)
	public String getActiveAgentSourcesAsString() {
		return String.join(", ", activeAgentSources);
	}

	@StringSetter(ACTIVE_AGENT_SOURCES)
	public void setActiveAgentSourcesAsString(String activeAgentSources) {
		this.activeAgentSources = interpretQSimComponents(this.activeAgentSources, activeAgentSources);
	}

	private List<String> interpretQSimComponents(List<String> initial, String config) {
		List<String> elements = Arrays.asList(config.split(",")).stream().map(String::trim)
				.collect(Collectors.toList());

		if (elements.size() == 1 && elements.get(0).length() == 0) {
			return new LinkedList<>();
		}

		return elements;
	}
}
