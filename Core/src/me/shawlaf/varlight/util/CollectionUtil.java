package me.shawlaf.varlight.util;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtil {

    private CollectionUtil() {
        throw new IllegalStateException("cannot create instance of util class");
    }

    public static <E> List<E> toList(E[] array) {
        List<E> list = new ArrayList<>(array.length);

        for (int i = 0; i < array.length; i++) {
            list.add(i, array[i]);
        }

        return list;
    }
}
