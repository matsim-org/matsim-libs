package org.matsim.application.options;

import picocli.CommandLine;

import java.util.regex.Pattern;

/**
 * Sample options that can be used as arg-group:
 *
 * <pre>{@code
 *     @CommandLine.Mixin
 *     private SampleOptions sample = new SampleOptions(1, 10, 25);
 * }
 * </pre>
 */
public final class SampleOptions {

	private static final Pattern PATTERN = Pattern.compile("\\d+pct");

	/**
	 * Needs to be present or the mixin will not be processed at all.
	 */
	@CommandLine.Option(names = "--unused-sample-option", hidden = true)
	private String unused;

	/**
	 * Available sample sizes
	 */
	private final int[] sizes;

	/**
	 * The selected sample size
	 */
	private int sample;

	/**
	 * Create Sample options with the available sample size.
	 * First sample size is the default option.
	 *
	 * @param sizes sizes in percent, e.g 1, 10, 25, etc..
	 */
	public SampleOptions(int... sizes) {
		this.sizes = sizes;
		this.sample = sizes[0];
	}

	@CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
	public void setSpec(CommandLine.Model.CommandSpec spec) {

		CommandLine.Model.ArgGroupSpec.Builder group = CommandLine.Model.ArgGroupSpec.builder()
				.exclusive(true)
				.heading("\nSample sizes:\n")
				.multiplicity("0..1");

		for (int i = 0; i < sizes.length; i++) {

			int size = sizes[i];

			CommandLine.Model.OptionSpec.Builder arg = CommandLine.Model.OptionSpec.
					builder(String.format("--%dpct", size))
					.type(Boolean.class)
					.order(i)
					.description("Run scenario with " + size + " pct sample size")
					.setter(new CommandLine.Model.ISetter() {
						@Override
						public <T> T set(T value) {
							setSize(size);
							return value;
						}
					})
					.defaultValue(i == 0 ? "true" : "false");

			group.addArg(arg.build());
		}

		spec.addArgGroup(group.build());
	}

	private void setSize(int sample) {
		this.sample = sample;
	}

	/**
	 * Returns the specified sample size.
	 */
	public int getSize() {
		return sample;
	}

	/**
	 * Adjust file name for selected sample size.
	 */
	public String adjustName(String name) {
		String postfix = getSize() + "pct";

		return PATTERN.matcher(name).replaceAll(postfix);
	}

}
