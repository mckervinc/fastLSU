/*
 * A Hash Table for monitoring the different chunks.
 * Handles concurrent synchronization for us.
*/
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

// private class that keeps track of the status of the variables
public class ResourceMonitor {

  private class Tuple {
    public int step;
    public double sum;

    public Tuple(int step, double sum) {
      this.step = step;
      this.sum = sum;
    }
  }

  private ConcurrentHashMap<String, Tuple> chm;
  private Tuple tuple;
  private int size;

  public ResourceMonitor(int numCores) {
    chm = new ConcurrentHashMap<String, Tuple>(numCores);
    for (int i = 1; i <= numCores; i++)
      chm.put("pool-1-thread-" + Integer.toString(i), new Tuple(-1, 0));
    size = numCores;
    tuple = new Tuple(-1, 0);
  }

  public int size() {
    return size;
  }

  public void update(String key, int step, double value) {
    chm.replace(key, new Tuple(step, value));
  }

  public Iterator<String> iterator() {
    return chm.keySet().iterator();
  }

  private Tuple getTuple(String key) {
    return chm.get(key);
  }

  public int getStep(String key) {
    return getTuple(key).step;
  }

  public double getValue(String key) {
    return getTuple(key).sum;
  }

  public double sum() {
    return tuple.sum;
  }

  public int sumStep() {
    return tuple.step;
  }

  public synchronized void changeSum(int step, double sum) {
    tuple.step = step;
    tuple.sum = sum;
  }

  public void remove(String key) {
    chm.remove(key);
  }
}