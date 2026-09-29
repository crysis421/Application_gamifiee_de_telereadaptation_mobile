package com.uphf.saes5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MuscleBodyGeometryTest {
    @Test
    public void groupAt_mapsFrontAnatomicalMusclesToTheirGroups() {
        assertEquals("Pectoraux", MuscleBodyGeometry.groupAt(0.28f, 0.31f));
        assertEquals("Tronc", MuscleBodyGeometry.groupAt(0.28f, 0.46f));
        assertEquals("Bras", MuscleBodyGeometry.groupAt(0.14f, 0.40f));
        assertEquals("Jambes", MuscleBodyGeometry.groupAt(0.24f, 0.68f));
        assertEquals("Jambes", MuscleBodyGeometry.groupAt(0.24f, 0.84f));
    }

    @Test
    public void groupAt_mapsBackAndHamstringsToTheirGroups() {
        assertEquals("Dos", MuscleBodyGeometry.groupAt(0.72f, 0.31f));
        assertEquals("Bras", MuscleBodyGeometry.groupAt(0.86f, 0.40f));
        assertEquals("Jambes", MuscleBodyGeometry.groupAt(0.68f, 0.62f));
        assertEquals("Jambes", MuscleBodyGeometry.groupAt(0.68f, 0.80f));
    }

    @Test
    public void groupAt_returnsNullOutsideTheBody() {
        assertNull(MuscleBodyGeometry.groupAt(0.05f, 0.05f));
        assertNull(MuscleBodyGeometry.groupAt(-0.1f, 0.5f));
        assertNull(MuscleBodyGeometry.groupAt(1.1f, 0.5f));
    }

    @Test
    public void groupAt_usesInclusiveLeftTopAndExclusiveRightBottomBounds() {
        assertEquals("Pectoraux", MuscleBodyGeometry.groupAt(0.20f, 0.27f));
        assertNull(MuscleBodyGeometry.groupAt(0.36f, 0.27f));
        assertEquals("Dos", MuscleBodyGeometry.groupAt(0.64f, 0.24f));
    }
}
