package signals.laemmer.model;

/**
 * Created by nkuehnel on 03.04.2017.
 */
public class LaemmerConfig {

    private double MAX_PERIOD = 120;
    private double DESIRED_PERIOD = 70;

    private boolean useBasicIntergreenTime = true;

    private double DEFAULZT_INTERGREEN = 5;

    public double getMAX_PERIOD() {
        return MAX_PERIOD;
    }

    public void setMAX_PERIOD(double MAX_PERIOD) {
        this.MAX_PERIOD = MAX_PERIOD;
    }

    public double getDESIRED_PERIOD() {
        return DESIRED_PERIOD;
    }

    public void setDESIRED_PERIOD(double DESIRED_PERIOD) {
        this.DESIRED_PERIOD = DESIRED_PERIOD;
    }

    public boolean isUseBasicIntergreenTime() {
        return useBasicIntergreenTime;
    }

    public void setUseBasicIntergreenTime(boolean useBasicIntergreenTime) {
        this.useBasicIntergreenTime = useBasicIntergreenTime;
    }

    public double getDEFAULZT_INTERGREEN() {
        return DEFAULZT_INTERGREEN;
    }

    public void setDEFAULZT_INTERGREEN(double DEFAULZT_INTERGREEN) {
        this.DEFAULZT_INTERGREEN = DEFAULZT_INTERGREEN;
    }


}
