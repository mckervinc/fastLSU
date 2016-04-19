/******************************************************************************
 * numCores instances of this class are instantiated. Overall, this file
 * does the algorithm - it performs linear scans over the entire chunk,
 * returns a new chunk that only contains significant p-values.
 ******************************************************************************/
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class WorkerThread implements Callable<PValues> {
  private double m, alpha;
  private PValues data;
  private String threadName;
  private static ResourceMonitor rm = FastBHConcurrent.rm;

  // constructor
  public WorkerThread(PValues data, double m, double alpha, String threadName) {
    this.data = data;
    this.m = m;
    this.alpha = alpha;
    this.threadName = threadName;
  }

  /*****************************************************************************/
  /*                            Main Functions                                 */
  /*****************************************************************************/

  /*
    The call() function is part of the Callable class. Gets called automatically
    while in parallel. This will ultimately perform the algorithm
  */
  public PValues call() throws Exception {
    
    // add the sum of a particular chunk to the resource monitor.
    notifier(0, data.prime);

    // finds the cumulative sum over multiple iterations
    double sum1 = rm.sum();
    double condition = sum1 * alpha/m;
    int step = 1;

    while(true) {
      double rciprime = kmax(condition, step);
      if (rciprime == 0.0) {
        rm.remove(threadName);
        return new PValues(0);
      }
      double sum2 = cumSum(step, rciprime);
      if (sum1 == sum2) {
        data.prime = rciprime;
        break;
      }
      condition = sum2 * alpha/m;
      sum1 = sum2;
      step++;
    }

    return data;
  }

  /*****************************************************************************/
  /*                           Signal Processing                               */
  /*****************************************************************************/

  private double cumSum(int step, double rci) {
    double sum = rci;
    for (Iterator<String> it = rm.iterator(); it.hasNext();) {
      String x = it.next();
      if (!x.equals(threadName)) {
        try {
          while (rm.getStep(x) != step) {
            if (rm.sumStep() == step) {
              notifier(step, rci);
              return rm.sum();
            }
          }
          sum += rm.getValue(x);
        }
        catch (NullPointerException e) {}
      }
    }
    notifier(step, rci);
    if (rm.sumStep() < step) rm.changeSum(step, sum);
    return sum;
  }

  private void notifier(int step, double rci) {
    rm.update(threadName, step, rci);
  }

  /*****************************************************************************/
  /*                            BH Calculations                                */
  /*****************************************************************************/

  // performs the algorithm
  private double kmax(double cond, int step) {
    double count = 0.0;
    for (int i = 0; i < data.array.length; i++) {
      double p = data.array[i];
      if (p != -1 && p < cond) count++;
      else data.array[i] = -1;
    }
    notifier(step, count);
    return count;
  }
}