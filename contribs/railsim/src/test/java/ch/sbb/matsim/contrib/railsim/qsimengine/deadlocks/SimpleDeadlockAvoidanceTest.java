package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleDeadlockAvoidanceTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Map<Id<RailResource>, SimpleDeadlockAvoidance.ConflictFreeLinks> computeConflictFreeLinks(String network) {
		Network net = NetworkUtils.readNetwork(Path.of("test/input/ch/sbb/matsim/contrib/railsim/integration", network).toString());
		RailResourceManager res = new RailResourceManagerImpl(EventsUtils.createEventsManager(), new RailsimConfigGroup(), net, new NoDeadlockAvoidance(null), Mockito.mock(TrainManager.class));
		return SimpleDeadlockAvoidance.computeConflictFreeLinks(net, res);
	}

	@Test
	void conflictFreeLinks() {

		Map<Id<RailResource>, SimpleDeadlockAvoidance.ConflictFreeLinks> result = computeConflictFreeLinks("parallelTracksNonStopingAreaRerouting/trainNetwork.xml");

		Id<RailResource> switch1and2 = Id.create("switch1and2", RailResource.class);
		Id<RailResource> switch3and4 = Id.create("switch3and4", RailResource.class);

		assertThat(result)
			.containsOnlyKeys(switch1and2, switch3and4)
			.hasEntrySatisfying(switch1and2,
				v -> assertThat(v.links())
					.hasSize(3)
					.allSatisfy(((k, v2) -> assertThat(v2).hasSize(2)))
			)
			.hasEntrySatisfying(switch3and4,
				v -> assertThat(v.links())
					.hasSize(3)
					.allSatisfy(((k, v2) -> assertThat(v2).hasSize(2)))
			);

	}
}
