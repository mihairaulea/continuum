import com.vividsolutions.jts.geom.Envelope;

/**
 * Created by user on 26/07/2016.
 */
public class TestUtils {

    public static double getRandomLat() {
        return Math.random() * Math.PI * 2;
    }

    public static double getRandomLon() {
        return Math.acos(Math.random() * 2 - 1);
    }

    public static double getRandomLatInEnvelope(Envelope envelope) {
        return envelope.getMinY() + Math.random() * (envelope.getMaxY()-envelope.getMinY());
    }

    public static double getRandomLonInEnvelope(Envelope envelope) {
        return envelope.getMinX() + Math.random()*(envelope.getMaxX() - envelope.getMinX());
    }

}
