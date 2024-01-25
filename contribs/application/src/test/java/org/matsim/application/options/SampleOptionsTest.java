package org.matsim.application.options;

import com.google.common.util.concurrent.AtomicDouble;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.util.concurrent.atomic.AtomicInteger;

public class SampleOptionsTest {

	@Test
	void flexible() {

		AtomicInteger size = new AtomicInteger();
		AtomicDouble dSize = new AtomicDouble();

		MATSimAppCommand command = new MATSimAppCommand() {

			@CommandLine.Mixin
			private SampleOptions sample;

			@Override
			public Integer call() throws Exception {
				size.set(sample.getSize());
				dSize.set(sample.getSample());
				return 0;
			}
		};

		command.execute("--sample-size", "0.5");

		Assertions.assertThat(size.get())
				.isEqualTo(50);

		Assertions.assertThat(dSize.get())
				.isEqualTo(0.5);
	}

	@Test
	void fixed() {

		AtomicInteger size = new AtomicInteger();

		MATSimAppCommand command = new MATSimAppCommand() {

			@CommandLine.Mixin
			private SampleOptions sample = new SampleOptions(10);

			@Override
			public Integer call() throws Exception {
				size.set(sample.getSize());
				return 0;
			}
		};

		command.execute("--10pct");

		Assertions.assertThat(size.get())
				.isEqualTo(10);
	}


}
