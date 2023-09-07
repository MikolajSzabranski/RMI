package rmicommunication;

import java.util.List;

public interface Ring<T> {
  List<T> getElements();

  void add(T element);

//  void delete(T element);

  void delete(String element);

  boolean ifEmpty();

  List<T> init(String[] stream);

  T getLast();
}