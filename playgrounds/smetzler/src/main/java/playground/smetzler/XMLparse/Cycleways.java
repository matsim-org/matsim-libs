package playground.smetzler.XMLparse;



public class Cycleways {
    private int id;
    private String cyclewaytype;
    private String cyclewaySurface;

     
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCyclewaytype() {
        return cyclewaytype;
    }
    public void setCyclewaytype(String cyclewaytype) {
        this.cyclewaytype = cyclewaytype;
    }
    public String getCyclewaySurface() {
        return cyclewaySurface;
    }
    public void setCyclewaySurface(String cyclewaySurface) {
        this.cyclewaySurface = cyclewaySurface;
    }
    
     
    @Override
    public String toString() {
        return "Cycleway-Links: ID="+this.id+" Cyclewaytype=" + this.cyclewaytype + " CyclewaySurface=" + this.cyclewaySurface;
    }
     
}