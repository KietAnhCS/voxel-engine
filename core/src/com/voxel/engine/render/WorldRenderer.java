package com.voxel.engine.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkListener;
import com.voxel.engine.world.ChunkScheduler;
import com.voxel.engine.world.ChunkState;
import com.voxel.engine.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorldRenderer implements ChunkListener {

    /**
     * So chunk duoc dua len GPU moi khung hinh. Mot lan upload ton ~3 ms tren luong chinh
     * (phan lon la dung cay BVH va cham), nen de 6 la tu tay minh ep khung hinh xuong ~50 fps
     * moi khi hang doi day. Ba chunk/khung hinh o 60 fps van la 180 chunk/giay - thua suc
     * theo kip nguoi choi chay.
     */
    private static final int UPLOAD_BUDGET_PER_FRAME = 3;
    private static final int MESH_TASKS_PER_FRAME = 4;

    private final World world;
    private final EngineConfig config;
    private final PerspectiveCamera camera;
    private final Material solidMaterial;
    private final Material fluidMaterial;
    private final ChunkScheduler scheduler = new ChunkScheduler();
    private final RenderCommandQueue commands = new RenderCommandQueue();
    private final Map<Chunk, ChunkMeshSet> meshSets = new ConcurrentHashMap<Chunk, ChunkMeshSet>();
    private final ExecutorService meshWorkers;
    private final ThreadLocal<ChunkMesher> meshers;
    private final AtomicInteger tasksInFlight = new AtomicInteger();
    private final RenderableProvider solidPass = new Pass(false);
    private final RenderableProvider translucentPass = new Pass(true);

    private CollisionSink collisionSink;
    private int visibleChunks;
    private int visibleSections;
    private int visibleTriangles;

    public WorldRenderer(World world, PerspectiveCamera camera, final TextureAtlas atlas,
                         Material solidMaterial, Material fluidMaterial) {
        this.world = world;
        this.config = world.config();
        this.camera = camera;
        this.solidMaterial = solidMaterial;
        this.fluidMaterial = fluidMaterial;

        final EngineConfig engineConfig = this.config;
        final BlockRegistry registry = world.registry();
        this.meshers = new ThreadLocal<ChunkMesher>() {
            @Override
            protected ChunkMesher initialValue() {
                return new ChunkMesher(engineConfig, registry, atlas);
            }
        };

        this.meshWorkers = Executors.newFixedThreadPool(config.workerThreads(), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "voxel-mesher-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public void attachCollisionSink(CollisionSink collisionSink) {
        this.collisionSink = collisionSink;
    }

    public RenderableProvider solidPass() {
        return solidPass;
    }

    public RenderableProvider translucentPass() {
        return translucentPass;
    }

    @Override
    public void onChunkGenerated(Chunk chunk) {
    }

    @Override
    public void onChunkLightChanged(Chunk chunk) {
        scheduler.submit(chunk, false, priorityOf(chunk));
    }

    @Override
    public void onChunkGeometryInvalid(Chunk chunk, boolean urgent) {
        scheduler.submit(chunk, urgent, priorityOf(chunk));
    }

    /**
     * Do uu tien = binh phuong khoang cach ngang tu camera toi tam chunk.
     * Dung binh phuong de khoi phai tinh can bac hai, va giu kieu int cho heap so sanh nhanh.
     */
    private int priorityOf(Chunk chunk) {
        int half = config.chunkSize() / 2;
        float dx = chunk.originX() + half - camera.position.x;
        float dz = chunk.originZ() + half - camera.position.z;
        return (int) (dx * dx + dz * dz);
    }

    @Override
    public void onChunkUnloaded(final Chunk chunk) {
        scheduler.forget(chunk);
        commands.submit(new RenderCommand() {
            @Override
            public void execute() {
                ChunkMeshSet set = meshSets.remove(chunk);
                if (set != null) {
                    set.dispose();
                }
                if (collisionSink != null) {
                    collisionSink.removeChunk(chunk);
                }
            }
        });
    }

    public void update() {
        commands.drain(UPLOAD_BUDGET_PER_FRAME);

        int dispatched = 0;
        while (dispatched < MESH_TASKS_PER_FRAME && tasksInFlight.get() < config.workerThreads() * 2) {
            final Chunk chunk = scheduler.poll();
            if (chunk == null) {
                break;
            }
            if (!chunk.isReadable()) {
                continue;
            }
            dispatched++;
            tasksInFlight.incrementAndGet();
            meshWorkers.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ChunkGeometryData data = meshers.get().build(world, chunk);
                        commands.submit(new UploadCommand(chunk, data));
                    } finally {
                        tasksInFlight.decrementAndGet();
                    }
                }
            });
        }
    }

    public int visibleChunks() {
        return visibleChunks;
    }

    public int visibleSections() {
        return visibleSections;
    }

    public int visibleTriangles() {
        return visibleTriangles;
    }

    public int pendingMeshUpdates() {
        return scheduler.pending();
    }

    public void dispose() {
        meshWorkers.shutdownNow();
        commands.clear();
        scheduler.clear();
        for (ChunkMeshSet set : meshSets.values()) {
            set.dispose();
        }
        meshSets.clear();
    }

    private final class UploadCommand implements RenderCommand {

        private final Chunk chunk;
        private final ChunkGeometryData data;

        private UploadCommand(Chunk chunk, ChunkGeometryData data) {
            this.chunk = chunk;
            this.data = data;
        }

        @Override
        public void execute() {
            ChunkMeshSet set = meshSets.get(chunk);
            if (set == null) {
                set = new ChunkMeshSet(data.sections(), chunk.originX(), chunk.originZ());
                meshSets.put(chunk, set);
            }

            for (int section = 0; section < data.sections(); section++) {
                if (set.isUnchanged(section, data.solidKey(section), data.translucentKey(section))) {
                    continue;
                }
                Mesh solid = createMesh(data.solidVertices(section), data.solidIndices(section));
                set.replaceSolid(section, solid);
                if (collisionSink != null) {
                    collisionSink.updateSection(chunk, section, solid, data.collisionIndexCount(section));
                }
                set.replaceTranslucent(section,
                        createMesh(data.translucentVertices(section), data.translucentIndices(section)));
            }

            chunk.markState(ChunkState.MESHED);
        }

        private Mesh createMesh(float[] vertices, short[] indices) {
            if (vertices == null || indices == null || indices.length == 0) {
                return null;
            }
            Mesh mesh = new Mesh(true,
                    vertices.length / MeshBuffer.FLOATS_PER_VERTEX,
                    indices.length,
                    VertexAttribute.Position(),
                    VertexAttribute.TexCoords(0),
                    VertexAttribute.Normal(),
                    VertexAttribute.ColorUnpacked());
            mesh.setVertices(vertices);
            mesh.setIndices(indices);
            return mesh;
        }
    }

    private final class Pass implements RenderableProvider {

        private final boolean translucent;

        private Pass(boolean translucent) {
            this.translucent = translucent;
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            int chunkCount = 0;
            int sectionCount = 0;
            int triangleCount = 0;

            for (Map.Entry<Chunk, ChunkMeshSet> entry : meshSets.entrySet()) {
                ChunkMeshSet set = entry.getValue();
                boolean chunkCounted = false;

                for (int section = 0; section < set.sections(); section++) {
                    Mesh mesh = translucent ? set.translucent(section) : set.solid(section);
                    if (mesh == null) {
                        continue;
                    }
                    BoundingBox bounds = translucent ? set.translucentBounds(section) : set.solidBounds(section);
                    if (bounds == null || !camera.frustum.boundsInFrustum(bounds)) {
                        continue;
                    }

                    Renderable renderable = pool.obtain();
                    renderable.material = translucent ? fluidMaterial : solidMaterial;
                    renderable.meshPart.id = "chunk";
                    renderable.meshPart.mesh = mesh;
                    renderable.meshPart.offset = 0;
                    renderable.meshPart.size = mesh.getNumIndices();
                    renderable.meshPart.primitiveType = GL20.GL_TRIANGLES;
                    renderable.worldTransform.set(set.transform());
                    renderable.environment = null;
                    renderables.add(renderable);

                    sectionCount++;
                    triangleCount += mesh.getNumIndices() / 3;
                    chunkCounted = true;
                }

                if (chunkCounted) {
                    chunkCount++;
                }
            }

            if (!translucent) {
                visibleChunks = chunkCount;
                visibleSections = sectionCount;
                visibleTriangles = triangleCount;
            }
        }
    }
}
