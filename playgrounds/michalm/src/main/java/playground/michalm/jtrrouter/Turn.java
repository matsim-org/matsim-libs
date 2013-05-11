package playground.michalm.jtrrouter;

/**
 * @author michalm
 */
public class Turn
{
    // structure
    /*package*/final int node;
    /*package*/final int prev;
    /*package*/final int[] next;
    /*package*/final double[] probs;

    // algorithm
    /*package*/ boolean visited;


    public Turn(int node, int prev, int[] next, double[] probs)
    {
        this.node = node;
        this.prev = prev;
        this.next = next;
        this.probs = probs;
    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prev).append(" -> ").append(node).append('\n');

        for (int i = 0; i < next.length; i++) {
            sb.append(next[i]).append(':').append(probs[i]).append('\n');
        }

        return sb.toString();
    }
}
