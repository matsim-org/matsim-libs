// code by varunpant
package playground.clib.gheat;

public class PointLatLng implements Comparable<PointLatLng> {
    private double weight;
    private double longitude;
    private double latitude;
    private Object opt_value;

    public Object getValue() {
        return opt_value;
    }

    public void setValue(Object opt_value) {
        this.opt_value = opt_value;
    }

    public PointLatLng(double longitude, double latitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public PointLatLng(double longitude, double latitude, double weight) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.weight = weight; // TODO is this being ack'ed
    }

    public PointLatLng(double longitude, double latitude, Object opt_value) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.opt_value = opt_value;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "(" + this.longitude + ", " + this.latitude + ")";
    }

    @Override
    public int compareTo(PointLatLng o) {
        PointLatLng tmp = (PointLatLng) o;
        if (this.longitude < tmp.longitude) {
            return -1;
        } else if (this.longitude > tmp.longitude) {
            return 1;
        } else {
            if (this.latitude < tmp.latitude) {
                return -1;
            } else if (this.latitude > tmp.latitude) {
                return 1;
            }
            return 0;
        }
    }
}
