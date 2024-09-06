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
	 * Available sample sizes
	 */
	private final int[] sizes;
	/**
	 * Needs to be present or the mixin will not be processed at all.
	 */
	@CommandLine.Option(names = "--unused-sample-option", hidden = true)
	private String unused;

	/**
	 * The selected sample size in (0, 1)
	 */
	private double sample;

	/**
	 * Whether sample size was set explicitly.
	 */
	private boolean set;

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

	/**
	 * Create sample options with arbitrary possible sample size. The sample size option will be indicated as required.
	 */
	public SampleOptions() {
		this.sizes = null;
		this.sample = 0;
	}

	@CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
	void setSpec(CommandLine.Model.CommandSpec spec) {

		// Build simple sample option without predefined sizes
		if (sizes == null) {
			CommandLine.Model.OptionSpec.Builder arg = CommandLine.Model.OptionSpec.
					builder("--sample-size")
					.type(Double.class)
					.description("Specify sample size fraction in (0, 1).")
					.setter(new CommandLine.Model.ISetter() {
						@Override
						public <T> T set(T value) {
							if (value == null)
								return null;

							setSize((double) value);
							return value;
						}
					})
					.required(true);

			spec.add(arg.build());

		} else {

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
								setSize(size / 100d);
								return value;
							}
						})
						.defaultValue(i == 0 ? "true" : "false");

				group.addArg(arg.build());
			}

			spec.addArgGroup(group.build());
		}
	}

	/**
	 * Returns the specified sample size as percent between 1 and 100
	 */
	public int getSize() {
		return (int) (sample * 100);
	}

	/**
	 * Return sample size as fraction between 0 and 1.
	 */
	public double getSample() {
		return sample;
	}

	/**
	 * Return factor that is used to upscale the sample size.
	 */
	public double getUpscaleFactor() {
		return 1.0 / sample;
	}

	private void setSize(double sample) {
		this.set = true;
		this.sample = sample;
	}

	/**
	 * Check whether the sample size was set explicitly.
	 */
	public boolean isSet() {
		return set;
	}

	/**
	 * Adjust file name for selected sample size if it was set explicitly.
	 */
	public String adjustName(String name) {
		if (!set) return name;

		String postfix = getSize() + "pct";

		return PATTERN.matcher(name).replaceAll(postfix);
	}

}
