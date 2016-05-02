package floetteroed.utilities.statisticslogging;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeStampStatistic<D extends Object> implements Statistic<D> {

	public static final String TIMESTAMP = "Timestamp";

	private final String label;

	public TimeStampStatistic() {
		this.label = TIMESTAMP;
	}

	public TimeStampStatistic(final String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return TIMESTAMP;
	}

	@Override
	public String value(final D data) {
		return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date(
				System.currentTimeMillis()));
	}
}
