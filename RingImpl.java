package rmicommunication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RingImpl<T> implements Ring<T> {

  private List<T> elements;

  @Override
  public List<T> getElements() {
    return elements;
  }

  public RingImpl() {
    elements = new ArrayList<>();
  }

  @Override
  public void add(T element) {
    elements.add(element);
  }

  @Override
  public void delete(String element) {
    elements.removeIf(e -> Objects.equals(e.toString(), element));
  }

  @Override
  public boolean ifEmpty() {
    return elements.isEmpty();
  }

  @Override
  public List<T> init(String[] list) {
    var listRepresentation = Arrays.stream(list).toList();
    elements = new ArrayList<>();
    for (String e : listRepresentation) {
      add((T) e);
    }
    return elements;
  }

  @Override
  public T getLast() {
    return elements.get(elements.size() - 1);
  }


}
