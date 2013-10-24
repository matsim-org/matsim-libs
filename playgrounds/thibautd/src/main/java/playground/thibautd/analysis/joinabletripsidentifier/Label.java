package playground.thibautd.analysis.joinabletripsidentifier;

class Label implements Comparable<Label> {
	private final double value;
	private final String prefix;
	private final String suffix;

	public Label(
			final double value,
			final String prefix,
			final String suffix) {
		this.value = value;
		this.prefix = prefix.intern();
		this.suffix = suffix.intern();
	}

	/**
	 * if the prefix and suffix are the same, compares the value; otherwise,
	 * compares string representation.
	 */
	@Override
	public int compareTo(final Label o) {
		if ( o.prefix.equals(prefix) && o.suffix.equals(suffix) ) {
			return Double.compare(value, o.value);
		}
		return toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return prefix + value + suffix;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		if ( o == null ) return false;
		return toString().equals(o.toString());
	}
}