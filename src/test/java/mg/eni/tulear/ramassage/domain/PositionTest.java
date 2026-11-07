package mg.eni.tulear.ramassage.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionTest {

    @Test
    void undegreDeLatitudeVautEnviron111Km() {
        double d = new Position(-23.0, 43.0).distanceVers(new Position(-24.0, 43.0));
        assertEquals(111.2, d, 0.5);
    }

    @Test
    void distanceVersSoiMemeEstNulle() {
        Position p = new Position(-23.35, 43.67);
        assertEquals(0.0, p.distanceVers(p), 1e-9);
    }
}
