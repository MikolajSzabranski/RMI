package rmicommunication;

import java.util.ArrayList;
import java.util.List;

public class Ring {
  private List<Integer> elements;

  public Ring() {
    elements = new ArrayList<>();
  }

  public void addElement(int element) {
    elements.add(element);
  }

  public int getLastElement() {
    return elements.get(elements.size() - 1);
  }
}