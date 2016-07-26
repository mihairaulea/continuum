import com.graphaware.module.timetree.TimeTree;
import com.graphaware.module.timetree.TimedEvents;
import com.graphaware.module.timetree.domain.Event;
import com.graphaware.module.timetree.domain.TimeInstant;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.EditableLayerImpl;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.encoders.SimplePointEncoder;
import org.neo4j.gis.spatial.pipes.GeoPipeline;
import org.neo4j.gis.spatial.pipes.processing.GeometryType;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by user on 25/07/2016.
 */
public class ContinuumTest {

    private GraphDatabaseService db;
    private SpatialDatabaseService spatial;
    private TimeTree timeTree;
    private static final DateTimeZone UTC = DateTimeZone.forTimeZone(TimeZone.getTimeZone("UTC"));
    private TimedEvents timedEvents;
    private Continuum continuum;

    @Before
    public void setUp() {
        // new GraphDatabaseFactory().newEmbeddedDatabase(new File("/Users/user/Documents/Neo4j/default.graphdb") );
        db =  new TestGraphDatabaseFactory().newImpermanentDatabase();
        continuum = new Continuum(db);
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

    // 1. add to geospatial
    // 2. create timenode for start and end
    // 3. create relationship to both
    // 4. query: a - get objects in area
    //           b - see if current time is interval defined by actor

    // addObject(GeoPostion position, HashMap properties, array[] timeAvailable)
    // addObject(GeoPostion position, HashMap properties, Date timeAvailable)
    // getObjectsAvailable(GeoPosition userPosition, int radius)

    // how does importing a shapefile work? what does it do?
    @Test
    public void addingMultipleContinuumObjectsShouldBuildAnRTree() {
        try(Transaction tx = db.beginTx()) {
            insertRandomContinuumNodes(100);
            ResourceIterable<Node> resourceIterable = db.getAllNodes();
            long noOfNodesWithBbox = resourceIterable.stream().filter(t -> t.hasProperty("bbox")).count();
            // r tree refference, plus the nodes i created
            Assert.isTrue(noOfNodesWithBbox == 101);
            tx.success();
        }
    }

    @Test
    public void testDataRetrieval() {
        try(Transaction tx = db.beginTx()) {
            insertRandomContinuumNodes(100);
            Envelope envelope = new Envelope(15.0, 16.0, 56.0, 57.0);
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            DateTime startDate = DateTime.now();
            DateTime endDate = DateTime.now().plusHours(4);
            insertRandomContinuumNodesInEnvelope(100, envelope);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );
            // all nodes are generated in the envelope, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 100);
            tx.success();
        }
    }

    private void insertRandomContinuumNodes(int noOfNodes) {
        for(int i=0;i<noOfNodes;i++) {
            Node continuumNode = db.createNode(Label.label("SimpleTest"));
            continuum.addContinuumCapabilitiesToNode(continuumNode, TestUtils.getRandomLat(), TestUtils.getRandomLon(), DateTime.now(), DateTime.now().plusDays(1) );
        }
    }

    private void insertRandomContinuumNodesInEnvelope(int noOfNodes, Envelope envelope) {
        for(int i=0;i<noOfNodes;i++) {
            Node continuumNode = db.createNode(Label.label("EnvelopeTest"));
            double lat = TestUtils.getRandomLatInEnvelope(envelope);
            double lon = TestUtils.getRandomLonInEnvelope(envelope);
            Assert.isTrue(envelope.contains(lon,lat));
            //System.out.println(lat+"  :  "+ lon + " " + envelope.contains(lat,lon));
            continuum.addContinuumCapabilitiesToNode(continuumNode, lat, lon, DateTime.now(), DateTime.now().plusDays(1) );
        }
    }


}
