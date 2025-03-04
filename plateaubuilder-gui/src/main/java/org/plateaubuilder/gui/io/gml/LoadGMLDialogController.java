package org.plateaubuilder.gui.io.gml;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.plateaubuilder.core.citymodel.helpers.SchemaHelper;
import org.plateaubuilder.core.editor.Editor;
import org.plateaubuilder.core.io.gml.GmlImporter;
import org.plateaubuilder.core.world.World;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LoadGMLDialogController {
    private Stage root;

    private List<File> gmlFiles;
    
    /**
     * FXMLのStageを設定
     * 
     * @param stage
     */
    public void setRoot(Stage stage) {
        root = stage;
    }

    private void setFile(List<File> files) {
        gmlFiles = files;
    }

    /**
     * 新規ボタン選択時イベント
     * @param actionEvent
     */
    public void onNewGML(ActionEvent actionEvent) {
        CoordinateDialogController.createCoorinateDialog(gmlFiles);
        root.close();
    }

    /**
     * 追加ボタン選択時イベント
     * 
     * @param actionEvent
     */
    public void onAddGML(ActionEvent actionEvent) {
        try {
            var world = World.getActiveInstance();
            var group = world.getCityModelGroup();
            World.getRoot3D().getChildren().removeAll(Editor.getXyzTile().getBaseMaps());
            for (var gmlFile : gmlFiles) {
                var cityModelView = GmlImporter.loadGml(group, gmlFile.toString(), World.getActiveInstance().getGeoReference().getEPSGCode());
                Editor.getXyzTile().loadAllBasemapImages(cityModelView.getGML().getBoundedBy().getEnvelope());

                // urfスキーマが読み込まれていない場合は読み込みを試行する
                if (Editor.getUrfSchemaDocument() == null) {
                    var schemaHandler = cityModelView.getSchemaHandler();
                    var urfSchema = SchemaHelper.getUrfSchema(schemaHandler);
                    var urfSchemaLocation = urfSchema == null ? null : SchemaHelper.getSchemaLocation(urfSchema);
                    if (urfSchemaLocation != null) {
                        Editor.settingUrfSchemaDocument(urfSchemaLocation);
                    }
                }

                group.addCityModel(cityModelView);
                world.addCityModel(cityModelView);
                // ツリー更新
                group.fireChangeEvent();
            }
            Editor.getXyzTile().updateBasemapVisibility();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        root.close();
    }

    /**
     * 座標系選択ダイアログ表示
     * 
     * @param files
     */
    public static void createLoadGMLDialog(List<File> files) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            FXMLLoader loader = new FXMLLoader(CoordinateDialogController.class.getResource("loadgml-dialog.fxml"));
            stage.setScene(new Scene(loader.load()));
            var controller = (LoadGMLDialogController) loader.getController();
            controller.setFile(files);
            controller.setRoot(stage);
            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
