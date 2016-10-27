package sebhoerl;

import static org.junit.Assert.*;

import org.junit.Test;

import playground.sebhoerl.av.utils.Grid;

public class TestGrid {
    @Test
    public void testGridExpansion() {
        Grid<Object> grid = new Grid<>(0.0, 0.0, 10.0, 10.0, 1.0, 1.0);
        
        grid.update(new Object(), 5.5, 5.5);
        grid.update(new Object(), 5.3, 5.5);
        grid.update(new Object(), 5.2, 5.5);
        grid.update(new Object(), 5.6, 5.5);
        
        assertEquals(4, grid.getClosest(5.4, 5.2, 2).size());
        
        grid.update(new Object(), 6.6, 5.5);
        
        assertEquals(4, grid.getClosest(5.4, 5.2, 2).size());
        assertEquals(5, grid.getClosest(5.4, 5.2, 10).size());
        assertEquals(1, grid.getClosest(6.4, 5.2, 1).size());
        
        grid.update(new Object(), 1.6, 5.5);
        
        assertEquals(6, grid.getClosest(5.4, 5.2, 1000).size());
    }
    
    @Test
    public void testGridUpdate() {
        Grid<Object> grid = new Grid<>(0.0, 0.0, 10.0, 10.0, 1.0, 1.0);
        
        Object object = new Object();
        
        grid.update(object, 5.5, 5.5);
        assertEquals(1, grid.getClosest(5.4, 5.4, 0).size());
        
        grid.update(object, 5.5, 2.5);
        assertEquals(0, grid.getClosest(5.4, 5.4, 0).size());
        assertEquals(1, grid.getClosest(5.4, 2.4, 0).size());
    }
    
    @Test
    public void testRemove() {
        Grid<Object> grid = new Grid<>(0.0, 0.0, 10.0, 10.0, 1.0, 1.0);
        
        Object object = new Object();
        
        grid.update(object, 5.5, 5.5);
        assertEquals(1, grid.getClosest(5.4, 5.4, 0).size());
        
        grid.remove(object);
        assertEquals(0, grid.getClosest(5.4, 5.4, 0).size());
    }
    
    @Test
    public void testBoundaries() {
        Grid<Object> grid = new Grid<>(0.0, 0.0, 10.0, 10.0, 1.0, 1.0);
        
        grid.update(new Object(), 0.0, 0.0);
        grid.update(new Object(), 10.0, 10.0);
    }
    
    @Test
    public void testDegenerateGrids() {
        // "Degrenerate grids"
        
        Grid<Object> grid = new Grid<>(0.0, 0.0, 10.0, 0.0, 1.0, 1.0);
        
        grid.update(new Object(), 2.0, 0.0);
        grid.update(new Object(), 4.0, 0.0);
        
        assertEquals(2, grid.getClosest(0.0, 0.0, 10).size());
        
        
        grid = new Grid<>(0.0, 0.0, 0.0, 10.0, 1.0, 1.0);
        
        grid.update(new Object(), 0.0, 2.0);
        grid.update(new Object(), 0.0, 4.0);
        
        assertEquals(2, grid.getClosest(0.0, 0.0, 10).size());
    }
}
