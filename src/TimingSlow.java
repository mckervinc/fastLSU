import java.io.*;

public class TimingSlow {

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
		double time = 0;

		String line;
		double[] res = null;
		System.out.println("===========================================================");
		System.out.println("Trial       ||     Load Time (ms)   || Proc Time (ms)");
		System.out.println("===========================================================");
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			BufferedReader br = new BufferedReader(new FileReader((new File(args[3])).getAbsolutePath()));
			while ((line=br.readLine()) != null) {
				String[] values = line.split(" ");
				for (int j = 0; j < values.length; j++) {
					data[pointer] = Double.parseDouble(values[j]);
					pointer++;			
				}
			}
			long mid = System.currentTimeMillis();
			String p = fixedLengthString(i+1, (mid-start));
			System.out.print(p);
			mid = System.currentTimeMillis();
			SlowBH sbh = new SlowBH(data, alpha);
			res = sbh.kmax();
			long end = System.currentTimeMillis();
			double timeinS = (double)(end - mid);
			System.out.println(timeinS);
			time += timeinS;
			br.close();
			pointer = 0;
		}
		double ms = time/((double)trials);
		System.out.println("===========================================================");
		if (res != null) printArr(res);
		System.out.println("===========================================================");
		System.out.printf("Average run time over %d Trials: %.2f ms, or %.8f s\n", trials, ms, ms/1000.0);
	}
}