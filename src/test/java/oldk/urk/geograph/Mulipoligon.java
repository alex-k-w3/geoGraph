package oldk.urk.geograph;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.Assert;

public class Mulipoligon {
    @Test
    public void covers() {
        GeometryFactory f = new GeometryFactory();


        Coordinate[] coords1 = {
                new Coordinate(0, 0), new Coordinate(100, 0), new Coordinate(100, 100), new Coordinate(0, 100), new Coordinate(0, 0)
        };

        Coordinate[] coords1_1 = {
                new Coordinate(20, 20), new Coordinate(30, 20), new Coordinate(30, 30), new Coordinate(20, 30), new Coordinate(20, 20)
        };


        Coordinate[] coords2 = {
                new Coordinate(1, 1), new Coordinate(2, 1), new Coordinate(2, 2), new Coordinate(1, 2), new Coordinate(1, 1)
        };
        Coordinate[] coords2_1 = {
                new Coordinate(20, 20), new Coordinate(30, 20), new Coordinate(29, 29), new Coordinate(21, 29), new Coordinate(20, 20)
        };

        var p1 = f.createPolygon(coords1);
        var p11 = f.createPolygon(coords1_1);
        var mp1 = f.createMultiPolygon(new Polygon[]{p1});
        var p2 = f.createPolygon(coords2);
        var p21 = f.createPolygon(coords2_1);
        var mp2 = f.createMultiPolygon(new Polygon[]{p2, p21});
        var r = mp1.contains(mp2);
        Assert.isTrue(r);
    }
}
