package signals;

/**
 * Created by nkuehnel on 05.04.2017.
 */
public interface Analyzable {

    public String getStatFields();
    public String getStepStats(double now);
    public boolean analysisEnabled();
}
