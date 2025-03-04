package org.plateaubuilder.core.world;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class CoordinateGrid {
    private Group grid;

    private SimpleBooleanProperty showGrid = new SimpleBooleanProperty(true) {
        @Override
        protected void invalidated() {
            if (get()) {
                if (grid == null) {
                    createGrid();
                }
                World.getRoot3D().getChildren().add(grid);
            } else if (grid != null) {
                World.getRoot3D().getChildren().remove(grid);
            }
        }
    };

    public boolean getShowGrid() {
        return showGrid.get();
    }

    public SimpleBooleanProperty showGridProperty() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid.set(showGrid);
    }

    private void createGrid() {
        grid = new Group();
        int gridSize = 100;
        double gridSpacing = 100.0;
        for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
            double position = i * gridSpacing;
            Line lineX = new Line(position, -gridSize * gridSpacing / 2, position, gridSize * gridSpacing / 2);
            Line lineY = new Line(-gridSize * gridSpacing / 2, position, gridSize * gridSpacing / 2, position);
            if (position == 0) {
                lineX.setStroke(Color.GREEN);
                lineY.setStroke(Color.RED);
            } else if (position % (100.0 * 10) == 0) {
                lineX.setStroke(Color.WHITESMOKE);
                lineY.setStroke(Color.WHITESMOKE);
            } else {
                lineX.setStroke(Color.GRAY);
                lineY.setStroke(Color.GRAY);
            }
            grid.getChildren().addAll(lineX, lineY);
        }
    }
}
