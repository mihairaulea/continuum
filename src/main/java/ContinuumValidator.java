import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

/**
 * Created by user on 03/08/2016.
 */
public class ContinuumValidator {

    public static boolean continuumNodeHasTimeReference(Node node) {
        return (node.hasRelationship(Direction.INCOMING, RelationshipType.withName("startTime")) && node.hasRelationship(Direction.INCOMING, RelationshipType.withName("endTime")))
                || node.hasRelationship(Direction.INCOMING, RelationshipType.withName("eventTime"));
    }

    public static boolean continuumNodeHasLocation(Node node) {
        return node.hasRelationship(Direction.INCOMING, RelationshipType.withName("RTREEREFERENCE")) && node.hasProperty("lat") && node.hasProperty("lon");
    }

}
