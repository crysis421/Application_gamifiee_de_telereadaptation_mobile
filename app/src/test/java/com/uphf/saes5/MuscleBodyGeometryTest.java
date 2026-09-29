package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MuscleBodyGeometryTest {
    @Test
    public void groupAt_mapsRepresentativeBodyPointsToMuscles() {
        assertEquals("Dos", MuscleBodyGeometry.groupAt(0.50f, 0.19f));
        assertEquals("Pectoraux", MuscleBodyGeometry.groupAt(0.50f, 0.30f));
        assertEquals("Bras", MuscleBodyGeometry.groupAt(0.24f, 0.38f));
        assertEquals("Tronc", MuscleBodyGeometry.groupAt(0.50f, 0.47f));
        assertEquals("Jambes", MuscleBodyGeometry.groupAt(0.43f, 0.78f));
    }

    @Test
    public void groupAt_returnsNullOutsideTheBody() {
        assertNull(MuscleBodyGeometry.groupAt(0.05f, 0.05f));
        assertNull(MuscleBodyGeometry.groupAt(-0.1f, 0.5f));
        assertNull(MuscleBodyGeometry.groupAt(1.1f, 0.5f));
    }

    @Test
    public void groupAt_usesInclusiveLeftTopAndExclusiveRightBottomBounds() {
        assertEquals("Dos", MuscleBodyGeometry.groupAt(0.30f, 0.16f));
        assertNull(MuscleBodyGeometry.groupAt(0.70f, 0.16f));
        assertEquals("Pectoraux", MuscleBodyGeometry.groupAt(0.50f, 0.24f));
    }
}
