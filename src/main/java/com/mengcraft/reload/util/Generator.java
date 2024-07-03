package com.mengcraft.reload.util;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Supplier;

public class Generator<T> implements Iterable<T> {

    private final Supplier<T> delegate;
    private Iterator<T> handle;

    public Generator(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        Preconditions.checkState(handle == null);
        return handle = new Handle();
    }

    class Handle implements Iterator<T> {

        T next;

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = delegate.get();
            }
            return next != null;
        }

        @Override
        public T next() {
            T old = next;
            next = null;
            return old;
        }
    }
}
