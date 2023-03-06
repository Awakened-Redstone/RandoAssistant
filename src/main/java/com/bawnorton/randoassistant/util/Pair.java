package com.bawnorton.randoassistant.util;

import java.io.Serializable;

public record Pair<A, B>(A a, B b) implements Serializable {
    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }
}
