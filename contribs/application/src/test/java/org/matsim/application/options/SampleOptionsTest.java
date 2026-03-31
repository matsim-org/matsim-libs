package org.matsim.application.options;

import com.google.common.util.concurrent.AtomicDouble;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.util.concurrent.atomic.AtomicReference;

public class SampleOptionsTest {

	@Test
	void flexible() {

		AtomicDouble size = new AtomicDouble();
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

		AtomicDouble size = new AtomicDouble();

		String name = "plans-42pct.xml";
		AtomicReference<String> adjustedName = new AtomicReference<>();

		MATSimAppCommand command = new MATSimAppCommand() {

			@CommandLine.Mixin
			private SampleOptions sample = new SampleOptions(10);

			@Override
			public Integer call() throws Exception {
				size.set(sample.getSize());
				adjustedName.set(sample.adjustName(name));
				return 0;
			}
		};

		command.execute("--10pct");

		Assertions.assertThat(size.get())
			.isEqualTo(10);

		Assertions.assertThat(adjustedName.get().equals("plans-10pct.xml")).isTrue();
	}


	@Test
	void smallSample() {

		AtomicDouble size = new AtomicDouble();
		AtomicDouble dSize = new AtomicDouble();

		String name = "plans-42pct.xml";
		AtomicReference<String> adjustedName = new AtomicReference<>();

		MATSimAppCommand command = new MATSimAppCommand() {

			@CommandLine.Mixin
			private SampleOptions sample;

			@Override
			public Integer call() throws Exception {
				size.set(sample.getSize());
				dSize.set(sample.getSample());
				adjustedName.set(sample.adjustName(name));
				return 0;
			}
		};

		command.execute("--sample-size", "0.001");

		Assertions.assertThat(size.get())
			.isEqualTo(0.1);

		Assertions.assertThat(dSize.get())
			.isEqualTo(0.001);

		Assertions.assertThat(adjustedName.get().equals("plans-0.1pct.xml")).isTrue();
	}
}
