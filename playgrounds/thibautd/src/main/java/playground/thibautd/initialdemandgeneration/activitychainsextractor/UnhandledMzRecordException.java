package playground.thibautd.initialdemandgeneration.activitychainsextractor;

/**
 * used to indicate that the activity chain is inconsistent, and should not be
 * retained.
 */
class UnhandledMzRecordException extends Exception {
	private static final long serialVersionUID = 1L;
	public UnhandledMzRecordException(final String msg) {
		super( msg );
	}
}