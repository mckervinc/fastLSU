/*
 * This class is a tuple. Gives access to the array as well as the number
 * of significant values (this is prime).
*/
public class PValues {
  public double[] array;
  public double prime;

  public PValues(double size) {
    array = new double[(int)size];
    prime = 0.0;
  }

  public int size() {
    return array.length;
  }
}