import java.io.*;
import java.util.*;

public class TimingFast {
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
		for (int i = 0; i < trials; i++) {
			FastBHConcurrent fbhc = new FastBHConcurrent(m, alpha, (new File(args[3])).getAbsolutePath());
			long start = System.currentTimeMillis();
			ArrayList<Double> data = fbhc.solver();
			printArrList(data);
			long end = System.currentTimeMillis();
			double timeinS = (double)(end - start);
			time += timeinS;
		}
		double ms = time/((double)trials);
		System.out.printf("Average run time over %d Trial(s): %.2f ms, or %.8f s\n", trials, ms, ms/1000.0);
	}
}