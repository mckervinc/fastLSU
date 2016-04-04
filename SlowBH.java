import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
public class SlowBH {

	private double m;
	private double[] pvalues;
	double alpha;
	// private double[] collection;

	public SlowBH(double[] arg, double alpha) {
		m = (double) arg.length;
		// long start = System.currentTimeMillis();
		Arrays.sort(arg);
		// long end = System.currentTimeMillis();
		// System.out.println("Sort time: " + (end - start) + " ms, or " + (double)(end - start)/1000.0 + " second(s)");
		pvalues = arg;
		this.alpha = alpha;
		// collection = new double[(int)m];
		// for (int i = 0; i < collection.length; i++)
			// collection[i] = ((double) (i+1))*alpha/m;
	}

	private void print(double[] arg, int x, String s) {
		for (int i = 0; i < x; i++)
			System.out.printf("%s%d]: %.11f\n", s, i, arg[i]);
	}

	/*
		Goes through the array, checking if the pvalues support the condition of
		being < alpha * kmax.r/m
	*/
	public double[] kmax() {
		int from = 0;
		int to = 0;

		// find from index, should be continuous since pvales
		// are sorted
		for (int i = 0; i < pvalues.length; i++) {
			if (pvalues[i] < ((double) (i+1))*alpha/m) {
				from = i;
				break;
			}
		}

		// find the to index
		for (int i = from+1; i < pvalues.length; i++) {
			if (pvalues[i] >= ((double) (i+1))*alpha/m) {
				to = i;
				break;
			}
		}

		pvalues = range(pvalues, 0, to);
		return pvalues;
	}

	// [from, to)
	private double[] range(double[] arg, int from, int to) {
		double[] result = new double[to - from];
		for (int i = from; i < to; i++)
			result[i - from] = arg[i];
		return result;	
	}

	public static void main(String[] args) {
		Random generator = new Random(1234);
		double[] nums = new double[10000];
		long start = System.currentTimeMillis();
		for (int i = 0; i < nums.length; i++)
			nums[i] = generator.nextDouble()/1000.0;
		long end = System.currentTimeMillis();
		double time = (end - start)/1000.0;
		System.out.println("Num Generator time: " + time + " seconds");
		SlowBH fbh = new SlowBH(nums, 0.1);
		start = System.currentTimeMillis();
		double[] result = fbh.kmax();
		end = System.currentTimeMillis();
		time = (end - start)/1000.0;
		System.out.println("SlowBH: " + time + " seconds");
	}
}