import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 * @author Sherry
 */
public class Rasterer {
    private double baseDPP;

    public Rasterer() {
        baseDPP = Math.abs((MapServer.ROOT_LRLON - MapServer.ROOT_ULLON)) / MapServer.TILE_SIZE;
    }

    private class Box {
        double longUL;
        double longLR;
        double latUL;
        double latLR;

        Box(double longUL, double longLR, double latUL, double latLR) {
            this.longUL = longUL;
            this.longLR = longLR;
            this.latUL = latUL;
            this.latLR = latLR;
        }
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    private int findDepth(double queryDPP) {
        double currDPP = baseDPP;
        int currDepth = 0;
        while (currDepth < 7 && currDPP > queryDPP) {
            currDepth += 1;
            currDPP /= 2;
        }
        return currDepth;
    }

    private Box findBounds(String filename) {
        int[] dxy = extractInfo(filename);
        int depth = dxy[0];
        int x = dxy[1];
        int y = dxy[2];
        double latSize = (MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT) / (Math.pow(2, depth));
        double longSize = (MapServer.ROOT_LRLON - MapServer.ROOT_ULLON) / (Math.pow(2, depth));
        double longUL = MapServer.ROOT_ULLON + longSize * x;
        double longLR = longUL + longSize;
        double latUL = MapServer.ROOT_ULLAT - latSize * y;
        double latLR = latUL - latSize;
        return new Box(longUL, longLR, latUL, latLR);
    }

    private int[] extractInfo(String filename) {
        int dInd = filename.indexOf('d');
        int imgDepth = Integer.parseInt(filename.substring(dInd + 1, dInd + 2));
        filename = filename.substring(dInd + 3);
        int xStart = filename.indexOf('x') + 1;
        int xEnd = filename.indexOf('_');
        int imgX = Integer.parseInt(filename.substring(xStart, xEnd));
        filename = filename.substring(xEnd + 1);
        int yStart = filename.indexOf('y') + 1;
        int yEnd = filename.indexOf('.');
        int imgY = Integer.parseInt(filename.substring(yStart, yEnd));
        int[] result = new int[]{imgDepth, imgX, imgY};
        return result;
    }

    private int[] tilesNeededX(double queryDPP, double ullon, double lrlon) {
        int xCount = 0;
        int depth = findDepth(queryDPP);
        int fileX = 0;
        String testFile = "d" + depth + "_x" + fileX + "_y0.png";
        Box testFileBox = findBounds(testFile);
        while (testFileBox.longLR < lrlon) {
            if (testFileBox.longLR >= ullon) {
                xCount += 1;
            }
            fileX += 1;
            testFile = "d" + depth + "_x" + fileX + "_y0.png";
            testFileBox = findBounds(testFile);
        }
        xCount += 1;
        int[] xTiles = new int[xCount];
        for (int i = 0; i < xCount; i++) {
            xTiles[xCount - i - 1] = fileX;
            fileX -= 1;
        }
        return xTiles;
    }

    private int[] tilesNeededY(double queryDPP, double ullat, double lrlat) {
        int yCount = 0;
        int depth = findDepth(queryDPP);
        int fileY = 0;
        String testFile = "d" + depth + "_x0_y" + fileY + ".png";
        Box testFileBox = findBounds(testFile);
        while (testFileBox.latLR > lrlat) {
            if (testFileBox.latLR <= ullat) {
                yCount += 1;
            }
            fileY += 1;
            testFile = "d" + depth + "_x0_y" + fileY + ".png";
            testFileBox = findBounds(testFile);
        }
        yCount += 1;
        int[] yTiles = new int[yCount];
        for (int i = 0; i < yCount; i++) {
            yTiles[yCount - i - 1] = fileY;
            fileY -= 1;
        }
        return yTiles;
    }

    private String[][] tilesArray(double queryDPP, double ullon, double lrlon,
                                  double ullat, double lrlat) {
        int depth = findDepth(queryDPP);
        int[] tilesY = tilesNeededY(queryDPP, ullat, lrlat);
        int[] tilesX = tilesNeededX(queryDPP, ullon, lrlon);
        String[][] tiles = new String[tilesY.length][tilesX.length];
        for (int i = 0; i < tilesY.length; i++) {
            for (int j = 0; j < tilesX.length; j++) {
                String filename = "d" + depth + "_x" + tilesX[j] + "_y" + tilesY[i] + ".png";
                tiles[i][j] = filename;
            }
        }
        return tiles;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    // ullon=-122.241632, lrlon=-122.24053, w=892.0, h=875.0, ullat=37.87655, lrlat=37.87548
    public Map<String, Object> getMapRaster(Map<String, Double> params) {

        Map<String, Object> results = new HashMap<>();
        double ullon = params.get("ullon");
        double lrlon = params.get("lrlon");
        double w = params.get("w");
        double h = params.get("h");
        double ullat = params.get("ullat");
        double lrlat = params.get("lrlat");
        double queryDPP = Math.abs(ullon - lrlon) / w;
        String[][] tiles = tilesArray(queryDPP, ullon, lrlon, ullat, lrlat);
        results.put("render_grid", tiles);
        Box firstTile = findBounds(tiles[0][0]);
        Box lastTile = findBounds(tiles[tiles.length - 1][tiles[0].length - 1]);
        results.put("raster_ul_lon", firstTile.longUL);
        results.put("raster_ul_lat", firstTile.latUL);
        results.put("raster_lr_lon", lastTile.longLR);
        results.put("raster_lr_lat", lastTile.latLR);
        results.put("depth", findDepth(queryDPP));
        results.put("query_success", validateParams(ullon, lrlon, ullat, lrlat));
        return results;
    }

    private boolean validateParams(double ullon, double lrlon, double ullat, double lrlat) {
        if (ullon >= lrlon || ullat <= lrlat) {
            return false;
        } else if (lrlon < MapServer.ROOT_ULLON || ullon > MapServer.ROOT_LRLON) {
            return false;
        } else {
            return !(lrlat > MapServer.ROOT_ULLAT || ullat < MapServer.ROOT_LRLAT);
        }
    }
}
