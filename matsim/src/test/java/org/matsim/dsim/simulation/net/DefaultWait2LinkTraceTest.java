package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The wait-to-link trace is a diagnostic switched on from the command line, so the one thing worth testing is that the switch is read
 * correctly - a typo there would otherwise silently produce a run with no output.
 */
class DefaultWait2LinkTraceTest {

	@AfterEach
	void clearProperty() {
		System.clearProperty(DefaultWait2Link.Trace.PROPERTY);
	}

	@Test
	void offWhenThePropertyIsUnset() {
		System.clearProperty(DefaultWait2Link.Trace.PROPERTY);
		assertThat(DefaultWait2Link.Trace.fromSystemProperty()).isNull();
	}

	@Test
	void readsLinkAndWindow() {
		System.setProperty(DefaultWait2Link.Trace.PROPERTY, "-391032875@79400-79420");

		DefaultWait2Link.Trace trace = DefaultWait2Link.Trace.fromSystemProperty();

		assertThat(trace).isNotNull();
		assertThat(trace.linkId()).isEqualTo("-391032875");
		assertThat(trace.from()).isEqualTo(79400);
		assertThat(trace.to()).isEqualTo(79420);
	}

	@Test
	void coversOnlyThatLinkAndWindow() {
		System.setProperty(DefaultWait2Link.Trace.PROPERTY, "-391032875@79400-79420");
		DefaultWait2Link.Trace trace = DefaultWait2Link.Trace.fromSystemProperty();

		Id<Link> traced = Id.createLinkId("-391032875");
		Id<Link> other = Id.createLinkId("-391032874");

		assertThat(trace.covers(traced, 79410)).isTrue();
		assertThat(trace.covers(traced, 79400)).isTrue();
		assertThat(trace.covers(traced, 79420)).isTrue();
		assertThat(trace.covers(traced, 79399)).isFalse();
		assertThat(trace.covers(traced, 79421)).isFalse();
		assertThat(trace.covers(other, 79410)).isFalse();
	}

	@Test
	void saysSoWhenTheSwitchIsMalformed() {
		System.setProperty(DefaultWait2Link.Trace.PROPERTY, "-391032875");

		assertThatThrownBy(DefaultWait2Link.Trace::fromSystemProperty)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining(DefaultWait2Link.Trace.PROPERTY);
	}
}
