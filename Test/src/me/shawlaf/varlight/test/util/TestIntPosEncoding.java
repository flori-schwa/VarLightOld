package me.shawlaf.varlight.test.util;

import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestIntPosEncoding {

    @Test
    public void testIntPosEncoding() {

        int[][] testData = new int[][]{
                {0, 0, 0},
                {1, 1, 1},
                {-2, 0, 10},
                {5, 255, -20},
                {1_000_000, 3000, -5_000_000},
                {-10, 0, -10}
        };

        for (int[] data : testData) {
            IntPosition position = new IntPosition(data[0], data[1], data[2]);
            long encoded = position.encode();
            IntPosition fromEncoded = new IntPosition(encoded);

            Assertions.assertEquals(position, fromEncoded);
        }

    }

}
