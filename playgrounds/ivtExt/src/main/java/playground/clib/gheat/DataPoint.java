// code by varunpant
package playground.clib.gheat;

public class DataPoint {
    private double x;
    private double y;
    /**
     * WARNING: WEIGHT IS EFFECTIVELY NOT USED ANYWHERE/CORRECTLY
     */
    private double weight;

    public DataPoint(double x, double y, double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
    }

    public DataPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
