
package nl.han.ica.datastructures;

import java.util.ArrayList;
import java.util.List;

public class HANStack<T> implements IHANStack<T> {
    private final List<T> elements = new ArrayList<>();

    @Override
    public void push(T value) {
        elements.add(value);
    }

    @Override
    public T pop() {
        if (elements.isEmpty()) {
            throw new IllegalStateException("Stack is leeg");
        }
        return elements.remove(elements.size() - 1);
    }

    @Override
    public T peek() {
        if (elements.isEmpty()) {
            throw new IllegalStateException("Stack is leeg");
        }
        return elements.get(elements.size() - 1);
    }
}
