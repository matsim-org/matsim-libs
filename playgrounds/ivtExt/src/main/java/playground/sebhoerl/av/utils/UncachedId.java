package playground.sebhoerl.av.utils;

public class UncachedId {
    private String id;
    
    public UncachedId(String id) {
        this.id = id;
    }
    
    public String get() {
        return id;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof UncachedId)) return false;
        return id.equals(((UncachedId) other).get());
    }
    
    @Override
    public String toString() {
        return id;
    }
    
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
