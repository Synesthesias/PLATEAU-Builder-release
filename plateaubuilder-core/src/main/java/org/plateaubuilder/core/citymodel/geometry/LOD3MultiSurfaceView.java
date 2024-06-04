package org.plateaubuilder.core.citymodel.geometry;

import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.plateaubuilder.core.utils3d.polygonmesh.TexCoordBuffer;
import org.plateaubuilder.core.utils3d.polygonmesh.VertexBuffer;

public class LOD3MultiSurfaceView extends AbstractMultiSurfaceMeshView {
    public LOD3MultiSurfaceView(MultiSurface gmlObject, VertexBuffer vertexBuffer, TexCoordBuffer texCoordBuffer) {
        super(gmlObject, 3, vertexBuffer, texCoordBuffer);
    }
}
