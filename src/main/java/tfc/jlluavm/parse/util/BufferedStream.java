package tfc.jlluavm.parse.util;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class BufferedStream<T> {
    public final int bufferLen;
    private DataLinkage<T> history;
    private boolean done = false;
    private ArrayList<T> future = new ArrayList<>();

    public BufferedStream(int bufferLen) {
        this.bufferLen = bufferLen;
        this.history = new DataLinkage<>(bufferLen);
    }

    public T peekHistory(int offset) {
        DataLinkage<T> curr = history;
        for (int i = 0; i < offset; i++)
            curr = curr.getPrev();
        return curr.get();
    }

    public T peekFuture(int index) {
        if (index == 0) return current();

        index -= 1;
        if (index < future.size()) {
            return future.get(index);
        }

        T t;
        boolean skip;
        do {
            t = $next();
            skip = $shouldSkip();
        } while (skip);

        future.add(t);

        return t;
    }

    public T current() {
        return history.get();
    }

    public void advance() {
        advance(1);
    }

    public void advance(int amount) {
        // TODO: fix optimized impl for future
//        int hRealLen = future.size();
//        int hlen = Math.min(hRealLen, bufferLen);
//        for (int i = hlen; i >= 1; i--) {
//            history = history.getNext();
//            history.set(future.get(future.size() - i));
//        }

//        for (int i = 0; i < (amount - hRealLen); i++) {
        for (int i = 0; i < amount; i++) {
            next();
        }
    }

    public T next() {
        if (!future.isEmpty()) {
            T t = future.remove(0);
            history = history.getNext();
            history.set(t);
            return t;
        }

        T t;
        boolean skip;
        do {
            t = $next();
            skip = $shouldSkip();
        } while (skip);
        history = history.getNext();
        history.set(t);

        if (t == null) done = true;

        return t;
    }

    protected abstract T $next();

    protected boolean $shouldSkip() {
        return false;
    }

    public void forEach(Consumer<T> consumer) {
        T t;
        while ((t = next()) != null) {
            consumer.accept(t);
        }
    }

    public boolean isDone() {
        return done;
    }
}
