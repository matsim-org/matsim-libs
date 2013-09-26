package pl.poznan.put.vrp.dynamic.data.network;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Iterators;


public class GrowingVrpGraph
    implements VrpGraph
{
    private final List<Vertex> vertices;
    private final List<Vertex> unmodifiableVertices;

    private final ArcFactory arcFactory;
    private final List<Map<Integer, Arc>> arcs;// fromVertex->toVertex->Arc


    // TODO: Map<Integer, Arc> ==> Map<Vertex, Arc> ???

    public GrowingVrpGraph(ArcFactory arcFactory)
    {
        vertices = new ArrayList<Vertex>();
        unmodifiableVertices = Collections.unmodifiableList(vertices);

        this.arcFactory = arcFactory;
        arcs = new ArrayList<Map<Integer, Arc>>();
    }


    @Override
    public int getVertexCount()
    {
        return vertices.size();
    }


    @Override
    public Vertex getVertex(int id)
    {
        return vertices.get(id);
    }


    @Override
    public List<Vertex> getVertices()
    {
        return unmodifiableVertices;
    }


    @Override
    public void addVertex(Vertex v)
    {
        if (v.getId() != vertices.size()) {
            throw new RuntimeException("ID must be equal to the list position");
        }

        vertices.add(v);
        arcs.add(null);
    }


    @Override
    public Arc getArc(Vertex vertexFrom, Vertex vertexTo)
    {
        int fromIdx = vertexFrom.getId();
        int toIdx = vertexTo.getId();

        Map<Integer, Arc> arcsFromVertexFrom = arcs.get(fromIdx);

        Arc arc;

        if (arcsFromVertexFrom == null) {
            arcsFromVertexFrom = new HashMap<Integer, Arc>();
            arcs.set(fromIdx, arcsFromVertexFrom);
            arc = null;
        }
        else {
            arc = arcsFromVertexFrom.get(toIdx);// autoboxing: toIdx
        }

        if (arc == null) {
            arc = arcFactory.createArc(vertexFrom, vertexTo);
            arcsFromVertexFrom.put(vertexTo.getId(), arc);
        }

        return arc;
    }


    @Override
    public Iterator<Arc> arcIterator()
    {
        return new GrowingVrpGraphArcIterator();
    }


    private class GrowingVrpGraphArcIterator
        implements Iterator<Arc>
    {
        private int fromVertexId;
        private Iterator<Entry<Integer, Arc>> entryIter;
        private Entry<Integer, Arc> nextEntry;


        public GrowingVrpGraphArcIterator()
        {
            fromVertexId = -1;
            entryIter = Iterators.emptyIterator();
            updateNextEntry();
        }


        @Override
        public Arc next()
        {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }

            Arc currentArc = nextEntry.getValue();
            updateNextEntry();
            return currentArc;
        }


        private void updateNextEntry()
        {
            if (entryIter.hasNext()) {
                nextEntry = entryIter.next();
                return;
            }

            while (++fromVertexId < vertices.size()) {
                Map<Integer, Arc> arcRow = arcs.get(fromVertexId);

                if (arcRow != null) {
                    entryIter = arcRow.entrySet().iterator();
                    nextEntry = entryIter.next(); // always at least one entry inside a row
                    return;
                }
            }

            nextEntry = null;
        }


        @Override
        public boolean hasNext()
        {
            return nextEntry != null;
        }


        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
