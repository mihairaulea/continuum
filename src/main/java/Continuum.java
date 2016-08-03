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
import org.neo4j.graphdb.spatial.Coordinate;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Can i use Gradoop-like batch processing for the intersection of time and space?
 * https://github.com/dbs-leipzig/gradoop
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

    // CREATE
    // should accomodate case where event is on a single date
    // should throw error if startTime > endTime
    // should throw error if lat, lon are not valid
    public Node addContinuumCapabilitiesToNode(Node node, double lat, double lon, DateTime startTime, DateTime endTime) {
        if(startTime.isAfter(endTime)) throw new Error("startTime can't be after endTime!");

        try (Transaction txContinuum = db.beginTx()) {
            node.addLabel(Label.label("Continuum"));
            node.setProperty("lat", lat);
            node.setProperty("lon", lon);

            // add object to timetree
            if(!startTime.equals(endTime)) {
                timedEvents.attachEvent(node, TimeRelationshipTypes.START_DATE, getTimeInstantFromDateTime(startTime));
                timedEvents.attachEvent(node, TimeRelationshipTypes.END_DATE, getTimeInstantFromDateTime(endTime));
            }
            else timedEvents.attachEvent(node, TimeRelationshipTypes.EVENT_DATE, getTimeInstantFromDateTime(startTime));
            // add object to spatial
            createOrRetrieveContinuumLayer().add(node);

            txContinuum.success();
        }
        return node;
    }

    // READ
    // envelope = new Envelope(15.0, 16.0, 56.0, 57.0)
    // should throw error if startTime < endTime
    // should throw error if Geometry is not valid
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

        TimeInstant today = getTimeInstantFromDateTime(startTime);
        TimeInstant tomorrow = getTimeInstantFromDateTime(endTime);

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

    // make sure each node that has a CONTINUUM label actually has all the relevant information: time and space points
    public List<Node> getAllContinuumNodes() {
        try(Transaction tx = db.beginTx()) {
            List<Node> continuumNodes = db.findNodes(Label.label("Continuum"))
                    .stream()
                    .filter(t -> ContinuumValidator.continuumNodeHasTimeReference(t) && ContinuumValidator.continuumNodeHasLocation(t))
                    .collect(Collectors.toList());
            tx.success();
            return continuumNodes;
        }
    }

    public List<TimeInstant> getAllTimePoints() {
        try(Transaction tx = db.beginTx()) {
            List<TimeInstant> timePoints =  db.findNodes(Label.label("Continuum"))
                    .stream()
                    .map(t -> getTimeInstantFromContinuumNode(t))
                    .collect(Collectors.toList());
            tx.success();
            return timePoints;
        }
    }

    // will i allow for both interval and fixed time events?
    // supports only DD-MM-YYYY format; research TimeTree, support all available dates
    private TimeInstant getTimeInstantFromContinuumNode(Node continuumNode) {
        int numberOfFriends = 0;
        DateTime dateTime = new DateTime();
        Traverser timeTraverser = getTimeTraverser( continuumNode );
        for ( Path timePath : timeTraverser )
        {
            Node endNode = timePath.endNode();
            Label label = endNode.getLabels().iterator().next();
            switch (label.name()) {
                case "Day":           { dateTime.withDayOfMonth((Integer)endNode.getProperty("value"));break;}
                case "Month":         { dateTime.withMonthOfYear((Integer)endNode.getProperty("value"));break;}
                case "Year":          { dateTime.withYear((Integer)endNode.getProperty("value"));break;}
                case "TimeTreeRoot:": { break;}
            }

        }
        return getTimeInstantFromDateTime(dateTime);
    }

    private Traverser getTimeTraverser( final Node continuumNode )
    {
        TraversalDescription td = db.traversalDescription()
                // depthFirst is more efficient - http://www.ekino.com/optimization-strategies-traversals-neo4j/
                .depthFirst()
                .relationships( RelationshipType.withName("child"), Direction.INCOMING )
                .evaluator( Evaluators.excludeStartPosition() );
        return td.traverse( continuumNode );
    }

    public List<Coordinate> getAllSpacePoints() {
        try(Transaction tx = db.beginTx()) {
            List<Coordinate> continuumNodes = db.findNodes(Label.label("Continuum"))
                    .stream()
                    .filter(t -> ContinuumValidator.continuumNodeHasTimeReference(t) && ContinuumValidator.continuumNodeHasLocation(t))
                    .map(t -> new Coordinate((Double)t.getProperty("lat"), (Double)t.getProperty("lon")))
                    .collect(Collectors.toList());
            tx.success();
            return continuumNodes;
        }
    }

    // UPDATE - should be handled by cypher

    // DELETE - if no point exists in space, should remove the RTREE
    //          if no point exists in time, should remove the time from the tree
    public void removeContinuumNode(long id) {

    }

    // utils
    public EditableLayer createOrRetrieveContinuumLayer() {
        return (EditableLayer) spatial.getOrCreateLayer("Continuum", SimplePointEncoder.class, EditableLayerImpl.class, "lon:lat");
    }

    // experimental
    public void clusterNodesBySpace() {

    }

    public void clusterNodesByTime() {

    }

    // give me the time-period with the most prolific artists
    public void summarizeDataByField() {

    }

    private TimeInstant getTimeInstantFromDateTime(DateTime dateTime) {
        return TimeInstant.instant(dateTime.getMillis());
    }

}
