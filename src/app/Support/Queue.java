package support;

import java.util.Vector;
import java.util.Enumeration;

public class Queue {
  Vector vector;

  public Queue() {
    vector = new Vector();
  }

  public void enqueue(Object object) {
    vector.addElement(object);
  }

  public Object dequeue() {
    Object dequeued_object = vector.firstElement();
    vector.removeElementAt(0);
    return dequeued_object;
  }
  
  public Enumeration elements() {
    return vector.elements();
  }

  public int length() {
    return vector.size();
  }
}
