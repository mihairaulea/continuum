import org.neo4j.graphdb.RelationshipType;

/**
 * Created by user on 03/08/2016.
 */
public class TimeRelationshipTypes {

    public final static RelationshipType START_DATE = RelationshipType.withName("startDate");
    public final static RelationshipType END_DATE = RelationshipType.withName("endDate");
    public final static RelationshipType EVENT_DATE = RelationshipType.withName("eventDate");

}
