/******************************************************************************
 * numCores instances of this class are instantiated. Overall, this file
 * does the algorithm - it performs linear scans over the entire chunk,
 * returns a new chunk that only contains significant p-values.
 ******************************************************************************/
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class MemoryThread implements Callable<PValues> {
  private double m, mci, alpha;
  private PValues data;

  // constructor
  public MemoryThread(PValues data, double m, double alpha) {
    this.data = data;
    this.m = m;
    mci = data.size();
    this.alpha = alpha;
  }

  /*****************************************************************************/
  /*                            Main Functions                                 */
  /*****************************************************************************/

  /*
    The call() function is part of the Callable class. Gets called automatically
    while in parallel. This will ultimately perform the algorithm
  */
  public PValues call() throws Exception {
    double condition = (m - mci + data.prime) * alpha/m;
    double rci = data.prime;

    while(true) {
      double rciprime = kmax(condition);
      if (rciprime == 0.0) return new PValues(0);
      if (rciprime == rci) {
        data.prime = rciprime;
        break;
      }
      condition = (m - mci + rciprime) * alpha/m;
      rci = rciprime;
    }

    return data;
  }

  /*****************************************************************************/
  /*                            BH Calculations                                */
  /*****************************************************************************/

  /*
    Goes through the array, checking if the pvalues support the condition of
    being < alpha * kmax.r/m
  */
  private double kmax(double cond) {
    double count = 0.0;
    for (int i = 0; i < data.array.length; i++) {
      double p = data.array[i];
      if (p != -1) {
        if (p < cond) count++;
        else data.array[i] = -1;
      }
    }
    return count;
  }
}