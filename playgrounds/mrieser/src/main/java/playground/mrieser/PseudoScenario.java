package playground.mrieser;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;

/**
 * Provides a real scenario, but exchanges the population.
 * Still, network and facilities can be reused that way.
 *
 * @author mrieser
 */
public class PseudoScenario implements Scenario {

	private final ScenarioImpl scenario;
	private Population myPopulation;

	public PseudoScenario(final ScenarioImpl scenario, final Population population) {
		this.scenario = scenario;
		this.myPopulation = population;
	}

	@Override
	public Population getPopulation() {
		return this.myPopulation;
	}

	@Override
	public Coord createCoord(double x, double y) {
		return this.scenario.createCoord(x, y);
	}

	@Override
	public Id createId(String string) {
		return this.scenario.createId(string);
	}

	@Override
	public Config getConfig() {
		return this.scenario.getConfig();
	}

	@Override
	public Network getNetwork() {
		return this.scenario.getNetwork();
	}

	@Override
	public void addScenarioElement(Object o) {
		this.scenario.addScenarioElement(o);
	}

	@Override
	public <T> T getScenarioElement(Class<? extends T> klass) {
		return this.scenario.getScenarioElement(klass);
	}

	@Override
	public boolean removeScenarioElement(Object o) {
		return this.scenario.removeScenarioElement(o);
	}

}
