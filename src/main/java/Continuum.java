import com.graphaware.module.timetree.SingleTimeTree;
import com.graphaware.module.timetree.TimeTree;
import com.graphaware.module.timetree.TimeTreeBackedEvents;
import com.graphaware.module.timetree.TimedEvents;
import com.graphaware.module.timetree.domain.Event;
import com.graphaware.module.timetree.domain.TimeInstant;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.gis.spatial.*;
import org.neo4j.gis.spatial.encoders.SimplePointEncoder;
import org.neo4j.gis.spatial.pipes.GeoPipeline;
import org.neo4j.graphdb.*;
import com.vividsolutions.jts.geom.Geometry;

import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 *
 */
public class Continuum {

    private GraphDatabaseService db;
    private SpatialDatabaseService spatial;
    private TimeTree timeTree;
    private static final DateTimeZone UTC = DateTimeZone.forTimeZone(TimeZone.getTimeZone("UTC"));
    private TimedEvents timedEvents;

    public Continuum(GraphDatabaseService service) {
        db = service;
        spatial = new SpatialDatabaseService(db);
        timeTree = new SingleTimeTree(db);
        timedEvents = new TimeTreeBackedEvents(timeTree);
    }

    public Node addContinuumCapabilitiesToNode(Node node, double lat, double lon, DateTime startTime, DateTime endTime) {
        try (Transaction txContinuum = db.beginTx()) {
            node.addLabel(Label.label("Continuum"));
            node.setProperty("lat", lat);
            node.setProperty("lon", lon);

            // add object to timetree
            timedEvents.attachEvent(node, RelationshipType.withName("startTime"), TimeInstant.instant(startTime.getMillis()));
            timedEvents.attachEvent(node, RelationshipType.withName("endTime"), TimeInstant.instant(endTime.getMillis()));

            // add object to spatial
            createOrRetrieveContinuumLayer().add(node);

            txContinuum.success();
        }
        return node;
    }

    // envelope = new Envelope(15.0, 16.0, 56.0, 57.0)
    public List<Node> getContinuumNodes(Geometry geometryToSearchIn, DateTime startTime, DateTime endTime) {
        List<Node> result = null;
        Layer layer = createOrRetrieveContinuumLayer();
        List<Node> spatialNodeList;
        try (Transaction txSpatial = db.beginTx()) {
            List<SpatialDatabaseRecord> spatialResults = GeoPipeline
                    .startWithinSearch(layer, geometryToSearchIn)
                    .toSpatialDatabaseRecordList();
            txSpatial.success();
            spatialNodeList = spatialResults.stream()
                                            .map(t -> t.getGeomNode())
                                            .collect(Collectors.toList());
        }

        TimeInstant today = TimeInstant.instant( startTime.getMillis() );
        TimeInstant tomorrow = TimeInstant.instant( endTime.getMillis() );

        try(Transaction txTemporal = db.beginTx()) {
            List<Event> temporalResults = timedEvents.getEvents(today, tomorrow);
            List<Node> temporalNodeList = temporalResults.stream()
                                                 .map(t -> t.getNode())
                                                 .collect(Collectors.toList());

            spatialNodeList.retainAll(temporalNodeList);
            result = spatialNodeList;
            txTemporal.success();
        }
        return result;
    }

    public EditableLayer createOrRetrieveContinuumLayer() {
        return (EditableLayer) spatial.getOrCreateLayer("Continuum", SimplePointEncoder.class, EditableLayerImpl.class, "lon:lat");
    }
}
