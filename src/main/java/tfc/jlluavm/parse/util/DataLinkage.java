package tfc.jlluavm.parse.util;

public class DataLinkage<T> {
    private DataLinkage<T> prev = null;
    private T element;
    private DataLinkage<T> next = null;

    public DataLinkage() {
    }

    public DataLinkage(int elementCount) {
        for (int i = 0; i < elementCount; i++) {
            add(null);
        }
    }

    public void add(T data) {
        if (next == null) {
            next = new DataLinkage<>();
            next.prev = this;
            next.next = this;
            next.element = data;
            prev = next;
            return;
        }

        DataLinkage<T> oldNext = next;
        DataLinkage<T> newElem = new DataLinkage<>();
        oldNext.prev = newElem;
        next = newElem;
        newElem.prev = this;
        newElem.next = oldNext;
        newElem.element = data;
    }

    public void remove() {
        next.prev = prev;
        prev.next = next;
    }

    public T get() {
        return element;
    }

    public void set(T data) {
        element = data;
    }

    public DataLinkage<T> getPrev() {
        return prev;
    }

    public DataLinkage<T> getNext() {
        return next;
    }
}
