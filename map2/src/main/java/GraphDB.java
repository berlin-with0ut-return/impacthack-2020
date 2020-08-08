import edu.princeton.cs.algs4.TrieST;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */
    HashMap<Long, HashSet<Long>> adjacency = new HashMap<>();
    HashMap<Long, Node> nodeIDs = new HashMap<>();
    TrieST<List<Long>> locations = new TrieST<>();

    public class Node {
        private long id;
        private double lat;
        private double lon;
        private String name;

        Node(long id, String name, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.name = name;
        }

        public String name() {
            return this.name;
        }

        public long id() {
            return this.id;
        }

        public double lat() {
            return this.lat;
        }

        public double lon() {
            return this.lon;
        }
    }

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

//    public void allLocations() {
//        locations = new TrieST();
//        for (GraphDB.Node n : nodeIDs.values()) {
//            String curNm = n.name();
//            String cleanNm = cleanString(curNm);
//            if (curNm.length() > 0) {
//                if (locations.contains(cleanNm)) {
//                    locations.get(cleanNm).add(n);
//                } else {
//                    ArrayList<Node> newEntry = new ArrayList<>();
//                    newEntry.add(n);
//                    locations.put(cleanNm, newEntry);
//                }
//            }
//        }
//    }

    public List<String> getLocationsByPrefix(String prefix) {
        ArrayList<String> matches = new ArrayList<>();
        for (String loc : locations.keysWithPrefix(prefix)) {
            List<Long> locList = locations.get(loc);
            for (Long nid : locList) {
                matches.add(nodeIDs.get(nid).name());
            }
        }
        return matches;
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        Set<Long> keys = new HashSet<>();
        for (long id : adjacency.keySet()) {
            keys.add(id);
        }
        for (long id: keys) {
            HashSet val = adjacency.get(id);
            if (val == null || val.isEmpty()) {
                adjacency.remove(id);
            }
        }
    }

    public List<Map<String, Object>> getLocations(String locationName) {
        List<Map<String, Object>> matchingLocs = new ArrayList<>();
        for (String ml : locations.keysThatMatch(cleanString(locationName))) {
            List<Long> nodeList = locations.get(ml);
            for (Long nid : nodeList) {
                Node n = nodeIDs.get(nid);
                HashMap<String, Object> params = new HashMap<>();
                params.put("lat", n.lat());
                params.put("lon", n.lon());
                params.put("name", n.name());
                params.put("id", n.id());
                matchingLocs.add(params);
            }
        }
        return matchingLocs;
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return adjacency.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return adjacency.get(v);
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long closestID = -1;
        double smallestDist = Integer.MAX_VALUE;
        for (long id : adjacency.keySet()) {
            Node currNode = nodeIDs.get(id);
            double currDist = distance(lon, lat, currNode.lon, currNode.lat);
            if (currDist < smallestDist) {
                closestID = id;
                smallestDist = currDist;
            }
        }
        return closestID;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return nodeIDs.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return nodeIDs.get(v).lat;
    }

    void addNode(String name, long id, double lat, double lon) {
        Node n = new Node(id, name, lat, lon);
        if (!adjacency.containsKey(id)) {
            adjacency.put(id, new HashSet<Long>());
            nodeIDs.put(id, n);
        }
    }

    void connect(long id1, long id2) {
        adjacency.get(id1).add(id2);
        adjacency.get(id2).add(id1);
    }

    void addName(long nodeID, String name) {
        if (nodeIDs.containsKey(nodeID)) {
            Node node = nodeIDs.get(nodeID);
            node.name = name;
            String curNm = node.name();
            String cleanNm = cleanString(curNm);
            if (curNm.length() > 0) {
                if (locations.contains(cleanNm)) {
                    locations.get(cleanNm).add(nodeID);
                } else {
                    ArrayList<Long> newEntry = new ArrayList<>();
                    newEntry.add(nodeID);
                    locations.put(cleanNm, newEntry);
                }
            }
        }
    }
}
