package com.graphhopper.routing.matrix;

import com.graphhopper.routing.DijkstraOneToMany;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DijkstraOneToManyMatrixCalculator implements MatrixCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DijkstraOneToManyMatrixCalculator.class);
    private final Graph queryGraph;
    private final Weighting weighting;
    private String debug;
    private int visitedNodes;

    public DijkstraOneToManyMatrixCalculator(Graph queryGraph, Weighting weighting) {
        this.queryGraph = queryGraph;
        this.weighting = weighting;
    }

    @Override
    public DistanceMatrix calcMatrix(MatrixSnapResult origins, MatrixSnapResult destinations) {
        StopWatch sw = new StopWatch().start();

        DijkstraOneToMany dij = new DijkstraOneToMany(queryGraph, weighting, TraversalMode.NODE_BASED);
        DistanceMatrix matrix = new DistanceMatrix(origins.size(), destinations.size(), origins.getPointsNotFound(), destinations.getPointsNotFound());
        for (int i = 0; i < origins.size(); i++) {
            for (int j = 0; j < destinations.size(); j++) {
                Path path = dij.calcPath(
                        origins.get(i).getClosestNode(),
                        destinations.get(j).getClosestNode()
                );
                matrix.setCell(i, j, path.getDistance(), path.getTime());
            }
            visitedNodes += dij.getVisitedNodes();
            dij.clear();
        }
        debug += ", " + dij.getName() + "-routing:" + sw.stop().getMillis() + " ms";
        logger.debug(debug);

        return matrix;
    }

    @Override
    public String getDebugString() {
        return debug;
    }

    @Override
    public int getVisitedNodes() {
        return visitedNodes;
    }
}
