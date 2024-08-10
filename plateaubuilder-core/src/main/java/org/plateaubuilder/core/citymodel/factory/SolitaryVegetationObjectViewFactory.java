package org.plateaubuilder.core.citymodel.factory;

import org.citygml4j.model.citygml.vegetation.SolitaryVegetationObject;
import org.plateaubuilder.core.citymodel.CityModelGroup;
import org.plateaubuilder.core.citymodel.CityModelView;
import org.plateaubuilder.core.citymodel.ManagedGMLView;
import org.plateaubuilder.core.citymodel.SolitaryVegetationObjectView;
import org.plateaubuilder.core.editor.Editor;

public class SolitaryVegetationObjectViewFactory extends AbstractFeatureViewFactory<SolitaryVegetationObject, SolitaryVegetationObject> {
    public SolitaryVegetationObjectViewFactory(CityModelGroup group, CityModelView target) {
        super(group, target);
    }

    @Override
    public ManagedGMLView<SolitaryVegetationObject> create(SolitaryVegetationObject gmlObject) {
        var view = new SolitaryVegetationObjectView(gmlObject);
        view.setId(gmlObject.getId());

        var lod1GeometryFactory = new LOD1GeometryFactory(getTarget());
        var lod1Geometry = lod1GeometryFactory.createLOD1Geometry(gmlObject);
        if (lod1Geometry != null) {
            view.setLOD1Geometry(lod1Geometry);
        }

        var lod2GeometryFactory = new LOD2GeometryFactory(getTarget());
        var lod2Geometry = lod2GeometryFactory.createLOD2Geometry(gmlObject);
        if (lod2Geometry != null) {
            view.setLOD2Geometry(lod2Geometry);
        }

        var lod3GeometryFactory = new LOD3GeometryFactory(getTarget());
        var lod3Geometry = lod3GeometryFactory.createLOD3Geometry(gmlObject);
        if (lod3Geometry != null) {
            view.setLOD3Geometry(lod3Geometry);
        }

        view.setDefaultVisible();

        view.toggleLODView(Editor.getCityModelViewMode().getLOD());

        return view;
    }
}
