package playground.sebhoerl.av.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Grid<BaseType> {
    final private double minX, minY, maxX, maxY, resolutionX, resolutionY;
    
    final private int cellCountX;
    final private int cellCountY;
    
    final private List<Collection<BaseType>> grid;
    final private Map<BaseType, Integer> map;
    
    public Grid(double minX, double minY, double maxX, double maxY, int cellsX, int cellsY) {
        this(minX, minY, maxX, maxY, 
                (maxX - minX) / (double)cellsX,
                (maxY - minY) / (double)cellsY);
    }
    
    public Grid(double minX, double minY, double maxX, double maxY, double resolutionX, double resolutionY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
        
        double distanceX = maxX - minX;
        double distanceY = maxY - minY;
        
        //assert(distanceX > 0.0);
        //assert(distanceY > 0.0);
        
        cellCountX = Math.max(1, (int)Math.ceil(distanceX / resolutionX));
        cellCountY = Math.max(1, (int)Math.ceil(distanceY / resolutionY));
        
        grid = new ArrayList<>(cellCountX * cellCountY);
        map = new HashMap<>();
        
        for (int i = 0; i < cellCountX * cellCountY; i++) {
            grid.add(null);
        }
    }
    
    public Collection<BaseType> getClosest(double x, double y, int k) {
        LinkedList<BaseType> items = new LinkedList<BaseType>();
        Set<Integer> indexSet = new HashSet<Integer>();
        
        // Guard for invalid coordinates
        int coordIndex = getCoordIndex(x, y);
        
        int baseX = getCellX(x - minX);
        int baseY = getCellY(y - minY);
        
        items.addAll(getCell(coordIndex));
        
        int iteration = 0;
        
        while (items.size() < k) {
            indexSet.clear();
            iteration++;
            
            int minX = baseX - iteration;
            int maxX = baseX + iteration;
            int minY = baseY - iteration;
            int maxY = baseY + iteration;
            
            if (minX < 0 && minY < 0 && maxX >= cellCountX && maxY >= cellCountY) {
                break;
            }
            
            if (minX >= 0) {
                for (int iy = Math.max(0, minY); iy <= Math.min(cellCountY - 1, maxY); iy++) {
                    indexSet.add(getCellIndex(minX, iy));
                }
            }
            
            if (maxX < cellCountX) {
                for (int iy = Math.max(0, minY); iy <= Math.min(cellCountY - 1, maxY); iy++) {
                    indexSet.add(getCellIndex(maxX, iy));
                }
            }
            
            if (minY >= 0) {
                for (int ix = Math.max(0, minX); ix <= Math.min(cellCountX - 1, maxX); ix++) {
                    indexSet.add(getCellIndex(ix, minY));
                }
            }
            
            if (maxY < cellCountY) {
                for (int ix = Math.max(0, minX); ix <= Math.min(cellCountX - 1, maxX); ix++) {
                    indexSet.add(getCellIndex(ix, maxY));
                }
            }
            
            for (Integer index : indexSet) {
                items.addAll(getCell(index));
            }
        }
        
        return items;
    }
    
    public void update(BaseType item, double x, double y) {
        Integer cachedIndex = map.get(item);
        Integer currentIndex = getCoordIndex(x, y);
        
        if (!currentIndex.equals(cachedIndex)) {
            if (cachedIndex != null) {
                getCell(cachedIndex).remove(item);
            }
            
            getCell(currentIndex).add(item);
            map.put(item, currentIndex);
        }
    }
    
    public void remove(BaseType item) {
        Integer cachedIndex = map.get(item);
        
        if (cachedIndex != null) {
            getCell(cachedIndex).remove(item);
            map.remove(item);
        }
    }
    
    private Collection<BaseType> getCell(int index) {
        Collection<BaseType> collection = grid.get(index);
        
        if (collection == null) {
            collection = new LinkedList<BaseType>();
            grid.set(index, collection);
        }
        
        return collection;
    }
    
    private int getCoordIndex(double x, double y) {
        if (x < minX || y < minY || x > maxX || y > maxY) {
            throw new IllegalArgumentException();
        }
        
        if (x == maxX) x = maxX - resolutionX * 0.5;
        if (y == maxY) y = maxY - resolutionY * 0.5;
        
        return getCellIndex(getCellX(x - minX), getCellY(y - minY));
    }
    
    private int getCellIndex(int x, int y) {
        return cellCountX * y + x;
    }
    
    private int getCellX(double x) {
        return (int)(x / this.resolutionX);
    }
    
    private int getCellY(double y) {
        return (int)(y / this.resolutionY);
    }
}
