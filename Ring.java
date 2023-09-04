package rmicommunication;

import java.util.ArrayList;
import java.util.List;

public class Ring {
  private List<Integer> elements;
  private int maxSize;

  public Ring(int maxSize) {
    elements = new ArrayList<>();
    this.maxSize = maxSize;
  }

  public void addElement(int element) {
    if (elements.size() == maxSize) {
      elements.remove(0);
    }
    elements.add(element);
  }

  public int getLastElement() {
    return elements.get(elements.size() - 1);
  }
}