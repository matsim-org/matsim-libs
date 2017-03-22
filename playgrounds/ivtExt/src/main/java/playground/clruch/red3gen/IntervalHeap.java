package playground.clruch.red3gen;

import java.util.Arrays;

/** An implementation of an implicit binary interval heap. */
public class IntervalHeap<Type> implements MinHeap<Type>, MaxHeap<Type> {
  private static final int defaultCapacity = 64;
  private Object[] data;
  private double[] keys;
  private int capacity;
  private int size;

  public IntervalHeap() {
    this(defaultCapacity);
  }

  public IntervalHeap(int capacity) {
    this.data = new Object[capacity];
    this.keys = new double[capacity];
    this.capacity = capacity;
    this.size = 0;
  }

  @Override
  public void offer(double key, Type value) {
    // If move room is needed, double array size
    if (size >= capacity) {
      capacity *= 2;
      data = Arrays.copyOf(data, capacity);
      keys = Arrays.copyOf(keys, capacity);
    }
    // Insert new value at the end
    size++;
    data[size - 1] = value;
    keys[size - 1] = key;
    siftInsertedValueUp();
  }

  @Override
  public void removeMin() {
    if (size == 0) {
      throw new IllegalStateException();
    }
    size--;
    data[0] = data[size];
    keys[0] = keys[size];
    data[size] = null;
    siftDownMin(0);
  }

  @Override
  public void replaceMin(double key, Type value) {
    if (size == 0) {
      throw new IllegalStateException();
    }
    data[0] = value;
    keys[0] = key;
    if (size > 1) {
      // Swap with pair if necessary
      if (keys[1] < key) {
        swap(0, 1);
      }
      siftDownMin(0);
    }
  }

  @Override
  public void removeMax() {
    if (size == 0) {
      throw new IllegalStateException();
    } else if (size == 1) {
      removeMin();
      return;
    }
    size--;
    data[1] = data[size];
    keys[1] = keys[size];
    data[size] = null;
    siftDownMax(1);
  }

  @Override
  public void replaceMax(double key, Type value) {
    if (size == 0) {
      throw new IllegalStateException();
    } else if (size == 1) {
      replaceMin(key, value);
      return;
    }
    data[1] = value;
    keys[1] = key;
    // Swap with pair if necessary
    if (key < keys[0]) {
      swap(0, 1);
    }
    siftDownMax(1);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Type getMin() {
    if (size == 0) {
      throw new IllegalStateException();
    }
    return (Type) data[0];
  }

  @Override
  @SuppressWarnings("unchecked")
  public Type getMax() {
    if (size == 0) {
      throw new IllegalStateException();
    } else if (size == 1) {
      return (Type) data[0];
    }
    return (Type) data[1];
  }

  @Override
  public double getMinKey() {
    if (size == 0) {
      throw new IllegalStateException();
    }
    return keys[0];
  }

  @Override
  public double getMaxKey() {
    if (size == 0) {
      throw new IllegalStateException();
    } else if (size == 1) {
      return keys[0];
    }
    return keys[1];
  }

  private int swap(int x, int y) {
    Object yData = data[y];
    double yDist = keys[y];
    data[y] = data[x];
    keys[y] = keys[x];
    data[x] = yData;
    keys[x] = yDist;
    return y;
  }

  /** Min-side (u % 2 == 0):
   * - leftchild: 2u + 2
   * - rightchild: 2u + 4
   * - parent: (x/2-1)&~1
   *
   * Max-side (u % 2 == 1):
   * - leftchild: 2u + 1
   * - rightchild: 2u + 3
   * - parent: (x/2-1)|1 */
  private void siftInsertedValueUp() {
    int u = size - 1;
    if (u == 0) {
      // Do nothing if it's the only element!
    } else if (u == 1) {
      // If it is the second element, just sort it with it's pair
      if (keys[u] < keys[u - 1]) { // If less than it's pair
        swap(u, u - 1); // Swap with it's pair
      }
    } else if (u % 2 == 1) {
      // Already paired. Ensure pair is ordered right
      int p = (u / 2 - 1) | 1; // The larger value of the parent pair
      if (keys[u] < keys[u - 1]) { // If less than it's pair
        u = swap(u, u - 1); // Swap with it's pair
        if (keys[u] < keys[p - 1]) { // If smaller than smaller parent pair
          // Swap into min-heap side
          u = swap(u, p - 1);
          siftUpMin(u);
        }
      } else {
        if (keys[u] > keys[p]) { // If larger that larger parent pair
          // Swap into max-heap side
          u = swap(u, p);
          siftUpMax(u);
        }
      }
    } else {
      // Inserted in the lower-value slot without a partner
      int p = (u / 2 - 1) | 1; // The larger value of the parent pair
      if (keys[u] > keys[p]) { // If larger that larger parent pair
        // Swap into max-heap side
        u = swap(u, p);
        siftUpMax(u);
      } else if (keys[u] < keys[p - 1]) { // If smaller than smaller parent pair
        // Swap into min-heap side
        u = swap(u, p - 1);
        siftUpMin(u);
      }
    }
  }

  private void siftUpMin(int c) {
    // Min-side parent: (x/2-1)&~1
    for (int p = (c / 2 - 1) & ~1; p >= 0 && keys[c] < keys[p]; c = p, p = (c / 2 - 1) & ~1) {
      swap(c, p);
    }
  }

  private void siftUpMax(int c) {
    // Max-side parent: (x/2-1)|1
    for (int p = (c / 2 - 1) | 1; p >= 0 && keys[c] > keys[p]; c = p, p = (c / 2 - 1) | 1) {
      swap(c, p);
    }
  }

  private void siftDownMin(int p) {
    for (int c = p * 2 + 2; c < size; p = c, c = p * 2 + 2) {
      if (c + 2 < size && keys[c + 2] < keys[c]) {
        c += 2;
      }
      if (keys[c] < keys[p]) {
        swap(p, c);
        // Swap with pair if necessary
        if (c + 1 < size && keys[c + 1] < keys[c]) {
          swap(c, c + 1);
        }
      } else {
        break;
      }
    }
  }

  private void siftDownMax(int p) {
    for (int c = p * 2 + 1; c <= size; p = c, c = p * 2 + 1) {
      if (c == size) {
        // If the left child only has half a pair
        if (keys[c - 1] > keys[p]) {
          swap(p, c - 1);
        }
        break;
      } else if (c + 2 == size) {
        // If there is only room for a right child lower pair
        if (keys[c + 1] > keys[c]) {
          if (keys[c + 1] > keys[p]) {
            swap(p, c + 1);
          }
          break;
        }
      } else if (c + 2 < size) {
        // If there is room for a right child upper pair
        if (keys[c + 2] > keys[c]) {
          c += 2;
        }
      }
      if (keys[c] > keys[p]) {
        swap(p, c);
        // Swap with pair if necessary
        if (keys[c - 1] > keys[c]) {
          swap(c, c - 1);
        }
      } else {
        break;
      }
    }
  }

  @Override
  public int size() {
    return size;
  }

  public int capacity() {
    return capacity;
  }

  @Override
  public String toString() {
    java.text.DecimalFormat twoPlaces = new java.text.DecimalFormat("0.00");
    StringBuffer str = new StringBuffer(IntervalHeap.class.getCanonicalName());
    str.append(", size: ").append(size()).append(" capacity: ").append(capacity());
    int i = 0, p = 2;
    while (i < size()) {
      int x = 0;
      str.append("\t");
      while ((i + x) < size() && x < p) {
        str.append(twoPlaces.format(keys[i + x])).append(", ");
        x++;
      }
      str.append("\n");
      i += x;
      p *= 2;
    }
    return str.toString();
  }

  @SuppressWarnings("unused")
  private boolean validateHeap() {
    // Validate left-right
    for (int i = 0; i < size - 1; i += 2) {
      if (keys[i] > keys[i + 1])
        return false;
    }
    // Validate within parent interval
    for (int i = 2; i < size; i++) {
      double maxParent = keys[(i / 2 - 1) | 1];
      double minParent = keys[(i / 2 - 1) & ~1];
      if (keys[i] > maxParent || keys[i] < minParent)
        return false;
    }
    return true;
  }
}