package org.plateau.citygmleditor.utils3d.polygonmesh;

import org.plateau.citygmleditor.utils3d.geom.Vec3f;

import java.util.ArrayList;
import java.util.List;

/**
 * ポリゴンメッシュを扱う汎用関数を提供します。
 */
public class PolygonMeshUtils {
    /**
     * ポリゴンメッシュ内の距離が0.01mより近い頂点を接合します。重複する頂点を削減し最適化するのに使用してください。
     *
     * @param vertexBuffer         入力頂点バッファ
     * @param outVertexBuffer      出力頂点バッファ。バッファが空でない場合、既に存在する頂点に入力頂点が追加されます。
     * @param outVertexIndexRemaps 接合前の頂点それぞれについて接合後の頂点インデックスを格納します。
     */
    public static void weldVertices(VertexBuffer vertexBuffer, VertexBuffer outVertexBuffer,
                                    List<Integer> outVertexIndexRemaps) {
        weldVertices(vertexBuffer, outVertexBuffer, outVertexIndexRemaps, 0.01f);
    }

    /**
     * ポリゴンメッシュ内の距離が近い頂点を接合します。重複する頂点を削減し最適化するのに使用してください。
     *
     * @param vertexBuffer         入力頂点バッファ
     * @param outVertexBuffer      出力頂点バッファ。バッファが空でない場合、既に存在する頂点に入力頂点が追加されます。
     * @param outVertexIndexRemaps 接合前の頂点それぞれについて接合後の頂点インデックスを格納します。
     * @param weldDistance         接合する頂点の最大距離
     */
    public static void weldVertices(VertexBuffer vertexBuffer, VertexBuffer outVertexBuffer,
                                    List<Integer> outVertexIndexRemaps, float weldDistance) {
        // TODO: Octree等使って高速化

        if (outVertexIndexRemaps == null)
            outVertexIndexRemaps = new ArrayList<>();
        else
            outVertexIndexRemaps.clear();

        for (int i = 0; i < vertexBuffer.getVertexCount(); ++i) {
            var vertex = vertexBuffer.getVertex(i);

            // 重複する頂点がないかチェック
            boolean isWelded = false;
            for (int j = 0; j < outVertexBuffer.getVertexCount(); ++j) {
                var otherVertex = outVertexBuffer.getVertex(j);

                // 重複する場合は出力先に頂点追加せずインデックス情報を保持
                if (vertex.distance(otherVertex) <= weldDistance) {
                    isWelded = true;
                    outVertexIndexRemaps.add(j);
                    break;
                }
            }
            if (!isWelded) {
                outVertexIndexRemaps.add(outVertexBuffer.getVertexCount());
                outVertexBuffer.addVertex(vertex);
            }
        }
    }

    /**
     * インデックスをリマップします。
     *
     * @param faceBuffer        入力面バッファ
     * @param vertexIndexRemaps インデックスのリマップ情報
     * @param outFaceBuffer     出力面バッファ。バッファが空でない場合、既に存在する面に入力面が追加されます。
     */
    public static void applyIndexRemap(FaceBuffer faceBuffer, FaceBuffer outFaceBuffer, List<Integer> vertexIndexRemaps) {

        // 頂点のリマップ情報をもとに面情報を格納
        for (int i = 0; i < faceBuffer.getPointCount(); ++i) {
            var originalVertexIndex = faceBuffer.getVertexIndex(i);
            if (originalVertexIndex >= vertexIndexRemaps.size()) {
                System.err.print("Invalid index.");
                continue;
            }

            outFaceBuffer.getBuffer().add(vertexIndexRemaps.get(originalVertexIndex));
            outFaceBuffer.getBuffer().add(faceBuffer.getNormalIndex(i));
            outFaceBuffer.getBuffer().add(faceBuffer.getTexCoordIndex(i));
        }
    }

    public static Vec3f calculateNormal(Vec3f p1, Vec3f p2, Vec3f p3) {
        Vec3f v1 = new Vec3f();
        v1.sub(p2, p1);
        Vec3f v2 = new Vec3f();
        v2.sub(p3, p1);
        var normal = new Vec3f();
        normal.cross(v1, v2);
        normal.normalize();

        return normal;
    }

    public static float[] calculateNormal(VertexBuffer vertexBuffer, FaceBuffer faceBuffer) {
        var normals = new float[faceBuffer.getPointCount() * 3];

        for (int i = 0; i < faceBuffer.getPointCount(); i += 3) {
            var normal = calculateNormal(
                    vertexBuffer.getVertex(faceBuffer.getVertexIndex(i)),
                    vertexBuffer.getVertex(faceBuffer.getVertexIndex(i + 1)),
                    vertexBuffer.getVertex(faceBuffer.getVertexIndex(i + 2))
            );
            for (int j = 0; j < 3; ++j) {
                normals[3 * (i + j)] = normal.x;
                normals[3 * (i + j) + 1] = normal.y;
                normals[3 * (i + j) + 2] = normal.z;
            }
        }

        return normals;
    }

}
