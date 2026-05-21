package st.rhapsody.voxelengine.lib.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import st.rhapsody.voxelengine.lib.terrain.World;
import st.rhapsody.voxelengine.lib.terrain.chunk.Chunk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoxelRender implements RenderableProvider {
    private final static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
    public Material material;
    private static int numberOfVertices = 0;
    private static int numberOfIndicies = 0;
    private static int numberOfVisibleChunks = 0;
    private static int numberOfChunks = 0;
    private static int blockCounter = 0;
    private static World terrain;
    private static int numberOfVisibleBlocks;
    private final PerspectiveCamera camera;
    private boolean alpha;
    private final Vector3 worldChunkPosition = new Vector3();
    private final Vector3 center = new Vector3();
    private final Vector3 dimensions = new Vector3();

    public VoxelRender(Material material, World terrain, PerspectiveCamera camera, boolean alpha) {
        this.material = material;
        this.terrain = terrain;
        this.camera = camera;
        this.alpha = alpha;
    }

    public static int getNumberOfVertices() { return numberOfVertices; }
    public static int getNumberOfIndicies() { return numberOfIndicies; }
    public static int getNumberOfChunks() { return numberOfChunks; }
    public static int getNumberOfVisibleChunks() { return numberOfVisibleChunks; }
    public static int getBlockCounter() { return blockCounter; }
    public static int getNumberOfVisibleBlocks() { return numberOfVisibleBlocks; }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        numberOfVertices = 0;
        numberOfIndicies = 0;
        numberOfVisibleChunks = 0;
        blockCounter = 0;
        numberOfVisibleBlocks = 0;
        numberOfChunks = 0;

        for (Chunk chunk : terrain.getChunks()) {
            numberOfChunks++;
            blockCounter += chunk.getBlockCounter();

            worldChunkPosition.set(chunk.getWorldPosition());
            boolean b = camera.frustum.sphereInFrustum(worldChunkPosition, World.WIDTH * 2f);
            if (!b) {
                chunk.setActive(false);
            } else {
                numberOfVisibleBlocks += chunk.getBlockCounter();
                numberOfVisibleChunks++;
                chunk.setActive(true);
            }

            Array<VoxelMesh> meshes = alpha ? chunk.getAlphaMeshes() : chunk.getMeshes();

            for (final BoxMesh boxMesh : meshes) {
                if (boxMesh == null) continue;

                Mesh mesh = boxMesh.getMesh();
                Mesh nonColliadableMesh = boxMesh.getnonColliadableMesh();

                if (mesh != null) {
                    BoundingBox boundingBox = boxMesh.getMeshBoundingBox();
                    boundingBox.getCenter(center);
                    boundingBox.getDimensions(dimensions);
                    worldChunkPosition.set(chunk.getWorldPosition()).add(center);
                    if (camera.frustum.boundsInFrustum(worldChunkPosition, dimensions)) {
                        addToRenderables(renderables, pool, mesh, boxMesh.getTransform());
                    }
                }

                if (nonColliadableMesh != null) {
                    BoundingBox boundingBox = boxMesh.getNonColliadableMeshBoundingBox();
                    boundingBox.getCenter(center);
                    boundingBox.getDimensions(dimensions);
                    worldChunkPosition.set(chunk.getWorldPosition()).add(center);
                    if (camera.frustum.boundsInFrustum(worldChunkPosition, dimensions)) {
                        addToRenderables(renderables, pool, nonColliadableMesh, boxMesh.getTransform());
                    }
                }

                if (boxMesh.needsRebuild() && !chunk.isRecalculating()) {
                    executorService.submit(() -> boxMesh.update());
                }
            }
        }
    }

    private void addToRenderables(Array<Renderable> renderables, Pool<Renderable> pool, Mesh mesh, Matrix4 transform) {
        if (mesh.getNumVertices() < 1 || mesh.getNumIndices() < 1) return;

        Renderable renderable = pool.obtain();
        renderable.material = material;
        renderable.meshPart.id = "mesh";
        renderable.meshPart.mesh = mesh;
        renderable.meshPart.offset = 0;
        renderable.meshPart.size = mesh.getNumIndices();
        renderable.meshPart.primitiveType = GL20.GL_TRIANGLES;
        renderable.worldTransform.set(transform);
        renderables.add(renderable);

        numberOfVertices += mesh.getNumVertices();
        numberOfIndicies += mesh.getNumIndices();
    }
}