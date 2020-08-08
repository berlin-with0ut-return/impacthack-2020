import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestAutocomplete {
    private static final String PARAMS_FILE = "path_params.txt";
    private static final String RESULTS_FILE = "path_results.txt";
    private static final int NUM_TESTS = 8;
    private static final String OSM_DB_PATH = "../library-sp18/data/berkeley-2018.osm.xml";
    private static GraphDB graph;
    private static boolean initialized = false;

    @Before
    public void setUp() throws Exception {
        if (initialized) {
            return;
        }
        graph = new GraphDB(OSM_DB_PATH);
        initialized = true;
    }

    @Test
    public void testCompletion() {
        List<String> startingTest = graph.getLocationsByPrefix("pe");
        float total = 0;
        for (int i = 0; i < 10; i++) {
            long startTime = System.nanoTime();
            List<String> test = graph.getLocationsByPrefix("pe");
            long endTime = System.nanoTime();

            float duration = (float) (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
            total += duration;
        }
        System.out.println(total/10);
    }

//    @Test
//    public void testSearch() {
//        List<Map<String, Object>> initTest = graph.getLocations("the rare barrel");
//        float total = 0;
//        for (int i = 0; i < 10; i++) {
//            long startTime = System.nanoTime();
//            List<Map<String, Object>> test = graph.getLocations("the rare barrel");
//            long endTime = System.nanoTime();
//
//            float duration = (float) (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
//            total += duration;
//        }
//        System.out.println(total/10);
//    }
}

