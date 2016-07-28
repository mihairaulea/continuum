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

    public static double getRandomLatOutsideEnvelope(Envelope envelope) {
        if(envelope.getMinY() == 0 && envelope.getMinY() == 90) throw new Error("Envelope covers full latitude, can not generate point outside");
        // return with probability 0.5 point either on the left or on the right of the latitude covered by the envelope
        return Math.random() % 2 == 0 ? (Math.random()*envelope.getMinY()) : (Math.random()*(envelope.getMinY()+envelope.getMaxY()));
    }

    public static double getRandomLonOutsideEnvelope(Envelope envelope) {
        // return with probability 0.5 point either on the bottom or on the top of the longitude covered by the envelope
        return Math.random() % 2 == 0 ? (Math.random()*envelope.getMinX()) : (Math.random()*(envelope.getMinX()+envelope.getMaxX()));
    }

}
