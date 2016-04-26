import java.io.*;
import java.util.*;

public class TimingSlow {

	public static final ArrayList<String> splitter(String line) {
	    line = line.trim();
	    ArrayList<String> arr = new ArrayList<String>();
	    boolean first = true;
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < line.length(); i++) {
	      char c = line.charAt(i);
	      switch(c) {
	        case '\t':
	        case ' ':
	          if (first) {
	            arr.add(sb.toString());
	            sb = new StringBuilder();
	          }
	          first = false;
	          break;
	        default:
	          first = true;
	          sb.append(c);
	          break;
	      }
	    }
	    arr.add(sb.toString());
	    return arr;
  	}

	public static String fixedLengthString(int trial, long mid) {
		String i = Integer.toString(trial);
		String j = Long.toString(mid);
		return String.format("%1$-12s||   %2$-19s||  ", i, j);
	}
	
	public static void printArr(double[] res) {
		for (int i = 0; i < res.length; i++)
			System.out.printf((i%5==4) ? "%.3e\n" : "%.3e ", res[i]);
		System.out.printf((res.length%5!=0) ? "\n": "");
	}

	public static void main(String[] args) throws Exception {
		int trials = Integer.parseInt(args[0]);
		int m = Integer.parseInt(args[2]);
		int pointer = 0;

		double alpha = Double.parseDouble(args[1]);
		double[] data = new double[m];
		double time = 0, loadTime = 0;

		String line;
		double[] res = null;
		boolean first = true;
		System.out.println("===========================================================");
		System.out.println("Trial       ||     Load Time (ms)   || Proc Time (ms)");
		System.out.println("===========================================================");
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			BufferedReader br = new BufferedReader(new FileReader((new File(args[3])).getAbsolutePath()));
			while ((line=br.readLine()) != null) {
				// String[] values = line.split("\\s+");
				ArrayList<String> values = splitter(line);
				for (int j = 0; j < values.size(); j++) {
					data[pointer] = Double.parseDouble(values.get(j));
					pointer++;			
				}
			}
			long mid = System.currentTimeMillis();
			long lt = mid - start;
			String p = fixedLengthString(i+1, lt);
			System.out.print(p);
			mid = System.currentTimeMillis();
			SlowBH sbh = new SlowBH(data, alpha);
			res = sbh.kmax();
			long end = System.currentTimeMillis();
			double timeinS = (double)(end - mid);
			System.out.println(timeinS);
			if (!first) {
				time += timeinS;
				loadTime += lt;
			}
			first = false;
			br.close();
			pointer = 0;
		}
		double ms = time/((double)trials-1.0);
		double ls = loadTime/((double)trials-1.0);
		System.out.println("===========================================================");
		if (res != null) {
			System.out.println("Result size: " + res.length);
			System.out.println("===========================================================");
			printArr(res);
		}
		System.out.println("===========================================================");
		System.out.printf("Average run time over %d Trials:  %.2f ms, or %.8f s\n", trials-1, ms, ms/1000.0);
		System.out.printf("Average load time over %d Trials: %.2f ms, or %.8f s\n", trials-1, ls, ls/1000.0);
	}
}