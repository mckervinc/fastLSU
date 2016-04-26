import java.util.ArrayList;

public class TableData {
	private String d1, d2, d3, d4, d5;
	
	public TableData(ArrayList<Double> list, double[] arr, int from, int to) {
		int c = 0;
		for (int i = from; i < to; i++) {
			switch(c) {
				case 0:
					if (list != null) d1 = list.get(i).toString();
					else d1 = Double.toString(arr[i]);
					c++;
					break;
				case 1:
					if (list != null) d2 = list.get(i).toString();
					else d2 = Double.toString(arr[i]);
					c++;
					break;
				case 2:
					if (list != null) d3 = list.get(i).toString();
					else d3 = Double.toString(arr[i]);
					c++;
					break;
				case 3:
					if (list != null) d4 = list.get(i).toString();
					else d4 = Double.toString(arr[i]);
					c++;
					break;
				case 4:
					if (list != null) d5 = list.get(i).toString();
					else d5 = Double.toString(arr[i]);
					c++;
					break;
			}
		}
	}

	public String getD1() {return d1;}
	public String getD2() {return d2;}
	public String getD3() {return d3;}
	public String getD4() {return d4;}
	public String getD5() {return d5;}

	public void setD1(String a) {d1 = a;}
	public void setD2(String a) {d1 = a;}
	public void setD3(String a) {d1 = a;}
	public void setD4(String a) {d1 = a;}
	public void setD5(String a) {d1 = a;}
}