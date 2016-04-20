import java.io.*;
import java.util.*;

public class TimingFast {

	public static String fixedLengthString(int trial, long mid) {
		String i = Integer.toString(trial);
		String j = Long.toString(mid);
		return String.format("%1$-12s||   %2$-19s||  ", i, j);
	}
	
	public static void printArrList(ArrayList<Double> data) {
		Collections.sort(data);
		for (int i = 0; i < data.size(); i++)
			System.out.printf((i%5==4) ? "%.3e\n" : "%.3e ", data.get(i));
		System.out.printf((data.size()%5!=0) ? "\n" : "");
	}

	public static void main(String[] args) throws Exception {
		int trials = Integer.parseInt(args[0]);
		int m = Integer.parseInt(args[2]);

		double alpha = Double.parseDouble(args[1]);
		double time = 0;
		ArrayList<Double> data = null;
		System.out.println("===========================================================");
		System.out.println("Trial       ||     Load Time (ms)   || Proc Time (ms)");
		System.out.println("===========================================================");
		for (int i = 0; i < trials; i++) {
			FastBHConcurrent fbhc = new FastBHConcurrent(m, alpha, (new File(args[3])).getAbsolutePath());
			if (fbhc.fitsInMem()) {
				long start = System.currentTimeMillis();
				PValues[] arr = fbhc.load();
				long mid = System.currentTimeMillis();
				String p = fixedLengthString(i+1, (mid-start));
				System.out.print(p);
				mid = System.currentTimeMillis();
				data = fbhc.solver(arr);
				long end = System.currentTimeMillis();
				double timeinS = (double)(end - mid);
				System.out.println(timeinS);
				time += timeinS;
			}
			else {
				while(!fbhc.isFinished()) {
					PValues[] arr = fbhc.loadInMem();
					data = fbhc.solverInMem(arr);
				}
			}
		}
		double ms = time/((double)trials);
		System.out.println("===========================================================");
		if (data != null) {
			printArrList(data);
			System.out.println("===========================================================");
			System.out.println("Result size: " + data.size());
		}
		System.out.println("===========================================================");
		System.out.printf("Average run time over %d Trial(s): %.2f ms, or %.8f s\n", trials, ms, ms/1000.0);
	}
}