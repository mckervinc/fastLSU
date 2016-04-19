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

  public PValues concatenate(PValues b) {
    if (this.array.length == 0) return b;
    if (b.array.length == 0) return this;
    PValues c = new PValues(this.array.length + b.array.length);
    System.arraycopy(this.array, 0, c.array, 0, this.array.length);
    System.arraycopy(b.array, 0, c.array, this.array.length, b.array.length);
    this.prime += b.prime;
    return c;
  }

  public int size() {
    return array.length;
  }
}