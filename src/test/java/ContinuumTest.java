import com.graphaware.module.timetree.TimeTree;
import com.graphaware.module.timetree.TimedEvents;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.util.Assert;

import java.io.File;
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
    private boolean permantent = true;

    @Before
    public void setUp() {
        // new GraphDatabaseFactory().newEmbeddedDatabase(new File("/Users/user/Documents/Neo4j/default.graphdb") );
        db =  permantent ? new GraphDatabaseFactory().newEmbeddedDatabase(new File("/Users/user/Documents/Neo4j/default.graphdb") ) : new TestGraphDatabaseFactory().newImpermanentDatabase() ;//
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
    public void addingOneContinuumNode() {
        try(Transaction tx = db.beginTx()) {
            insertRandomContinuumNodes(1, DateTime.now(),DateTime.now().plusHours(2));
            tx.success();
        }
    }

    @Test
    public void addingMultipleContinuumObjectsShouldBuildAnRTree() {
        try(Transaction tx = db.beginTx()) {
            insertRandomContinuumNodes(100,DateTime.now(), DateTime.now().plusDays(1));
            ResourceIterable<Node> resourceIterable = db.getAllNodes();
            long noOfNodesWithBbox = resourceIterable.stream().filter(t -> t.hasProperty("bbox")).count();
            // r tree refference, plus the nodes i created
            Assert.isTrue(noOfNodesWithBbox == 101);
            tx.success();
        }
    }

    @Test
    public void shouldRetreiveAllDataFromEnvelope() {
        try(Transaction tx = db.beginTx()) {
            // create
            Envelope envelope = new Envelope(15.0, 16.0, 56.0, 57.0);
            insertRandomContinuumNodesInEnvelope(100, envelope,DateTime.now(), DateTime.now().plusDays(1));

            // query
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            DateTime startDate = DateTime.now();
            DateTime endDate = DateTime.now().plusHours(4);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );
            // all nodes are generated in the envelope, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 100);
            tx.success();
        }
    }

    @Test
    public void shouldRetrievePartialDataFromEnvelope() {
        try(Transaction tx = db.beginTx()) {
            // create
            Envelope envelope = new Envelope(15.0, 16.0, 56.0, 57.0);
            insertRandomContinuumNodesInEnvelope(50, envelope, DateTime.now(), DateTime.now().plusDays(1));
            insertRandomContinuumNodesOutsideEnvelope(50, envelope, DateTime.now(), DateTime.now().plusDays(1));

            // query
            DateTime startDate = DateTime.now().plusHours(1);
            DateTime endDate = DateTime.now().plusHours(4);
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );

            // assert
            // half of the nodes are generated in the envelope, half outside, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 50);
            tx.success();
        }
    }

    @Test
    public void shouldNotRetrieveAnyDataFromEnvelope() {
        try(Transaction tx = db.beginTx()) {
            // create
            Envelope envelope = new Envelope(15.0, 16.0, 56.0, 57.0);
            insertRandomContinuumNodesOutsideEnvelope(100, envelope, DateTime.now(), DateTime.now().plusDays(1));

            // query
            DateTime startDate = DateTime.now().plusHours(1);
            DateTime endDate = DateTime.now().plusHours(4);
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );

            // assert
            // half of the nodes are generated in the envelope, half outside, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 0);
            tx.success();
        }
    }


    @Test
    public void shouldRetreiveAllDataInTimeframe() {
        try(Transaction tx = db.beginTx()) {
            // create - make space not an issue
            Envelope envelope = new Envelope(-90, 90, -180, 180);
            insertRandomContinuumNodes(100, DateTime.now(), DateTime.now().plusDays(1));

            // query
            DateTime startDate = DateTime.now().plusHours(1);
            DateTime endDate = DateTime.now().plusHours(4);
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );

            // assert
            // half of the nodes are generated in the envelope, half outside, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 100);
            tx.success();
        }
    }

    @Test
    public void shouldRetrievePartialDataInTimeframe() {
        try(Transaction tx = db.beginTx()) {
            // create - make space not an issue
            Envelope envelope = new Envelope(-90, 90, -180, 180);
            // these are the nodes that should be retrieved
            insertRandomContinuumNodes(50, DateTime.now(), DateTime.now().plusDays(1));
            // these are the nodes that should not be retrieved
            insertRandomContinuumNodes(50, DateTime.now().minusDays(3), DateTime.now().minusDays(3));

            // query
            DateTime startDate = DateTime.now().plusHours(1);
            DateTime endDate = DateTime.now().plusHours(4);
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );

            // assert
            // half of the nodes are generated in the envelope, half outside, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 100);
            tx.success();
        }
    }

    @Test
    public void shouldNotRetrieveAnyDataInTimeframe() {
        try(Transaction tx = db.beginTx()) {
            // create - make space not an issue
            Envelope envelope = new Envelope(-90, 90, -180, 180);
            // these are the nodes that should not be retrieved
            insertRandomContinuumNodes(100, DateTime.now().minusDays(3), DateTime.now().minusDays(3));

            // query
            DateTime startDate = DateTime.now().plusHours(1);
            DateTime endDate = DateTime.now().plusHours(4);
            Geometry toSearchIn = continuum.createOrRetrieveContinuumLayer().getGeometryFactory().toGeometry(envelope);
            List<Node> retrievedNodes = continuum.getContinuumNodes( toSearchIn, startDate, endDate );

            // assert
            // half of the nodes are generated in the envelope, half outside, and in a time contained within the search time
            Assert.isTrue(retrievedNodes.size() == 0);
            tx.success();
        }
    }


    private void insertRandomContinuumNodes(int noOfNodes, DateTime startDate, DateTime endDate) {
        for(int i=0;i<noOfNodes;i++) {
            Node continuumNode = db.createNode(Label.label("SimpleTest"));
            continuum.addContinuumCapabilitiesToNode(continuumNode, TestUtils.getRandomLat(), TestUtils.getRandomLon(), startDate, endDate );
        }
    }

    private void insertRandomContinuumNodesInEnvelope(int noOfNodes, Envelope envelope, DateTime startDate, DateTime endDate) {
        for(int i=0;i<noOfNodes;i++) {
            Node continuumNode = db.createNode(Label.label("EnvelopeTest"));
            double lat = TestUtils.getRandomLatInEnvelope(envelope);
            double lon = TestUtils.getRandomLonInEnvelope(envelope);
            Assert.isTrue(envelope.contains(lon,lat));
            //System.out.println(lat+"  :  "+ lon + " " + envelope.contains(lat,lon));
            continuum.addContinuumCapabilitiesToNode(continuumNode, lat, lon, DateTime.now(), DateTime.now().plusDays(1) );
        }
    }

    private void insertRandomContinuumNodesOutsideEnvelope(int noOfNodes, Envelope envelope, DateTime startDate, DateTime endDate) {
        for(int i=0;i<noOfNodes;i++) {
            Node continuumNode = db.createNode(Label.label("EnvelopeTest"));
            double lat = TestUtils.getRandomLatOutsideEnvelope(envelope);
            double lon = TestUtils.getRandomLonOutsideEnvelope(envelope);
            Assert.isTrue(!envelope.contains(lon,lat));
            //System.out.println(lat+"  :  "+ lon + " " + envelope.contains(lat,lon));
            continuum.addContinuumCapabilitiesToNode(continuumNode, lat, lon, DateTime.now(), DateTime.now().plusDays(1) );
        }
    }


}
