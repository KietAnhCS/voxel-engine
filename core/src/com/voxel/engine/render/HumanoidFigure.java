package com.voxel.engine.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * Mot hinh nguoi kieu Minecraft san sang de ve, gom sau khoi hop rieng biet:
 *
 * <pre>
 *          head        (dau)
 *   armLeft body armRight   (tay trai - than - tay phai)
 *      legLeft legRight     (chan trai - chan phai)
 * </pre>
 *
 * Moi khoi la mot NODE rieng, nen chi can xoay node quanh khop (hong voi chan, vai voi tay)
 * la nhan vat buoc di: chan trai dua ra truoc thi chan phai lui ve sau, tay vung nguoc chieu
 * chan - dung nhu Minecraft ban cu.
 *
 * <p>Cach dung: {@link #begin} mot lan, roi voi TUNG nhan vat goi {@link #pose} + {@link #draw},
 * cuoi cung {@link #end}. Ca dam quai vat / nguoi choi khac dung chung mot hinh duy nhat,
 * chi dat lai vi tri truoc moi lan ve (mau FLYWEIGHT) nen rat re.
 */
public final class HumanoidFigure implements Disposable {

    /** Chieu cao nhan vat (khoi), khop voi hinh va cham cua nguoi choi. */
    public static final float HEIGHT = 1.75f;
    /** Mot don vi 1/16 khoi cua skin sau khi thu nho. */
    private static final float U = HEIGHT / 32f;

    /** Khop hong: chan xoay quanh cao do nay (don vi skin, tinh tu ban chan). */
    private static final float HIP_Y = 12f;
    /** Khop vai: tay xoay quanh cao do nay. */
    private static final float SHOULDER_Y = 24f;

    /** Bon khuc xoay duoc, theo dung thu tu nay: chan trai, chan phai, tay trai, tay phai. */
    private static final String[] LIMB_NAMES = {"legLeft", "legRight", "armLeft", "armRight"};
    private static final int LEG_LEFT = 0;
    private static final int LEG_RIGHT = 1;
    private static final int ARM_LEFT = 2;
    private static final int ARM_RIGHT = 3;

    private final Texture skin;
    private final Model model;
    private final ModelInstance instance;
    /** Node cua tung tay chan, tim mot lan luc dau thay vi do ten moi khung hinh. */
    private final Node[] limbs = new Node[LIMB_NAMES.length];
    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();

    /**
     * @param skinPath duong dan file skin 64x64 trong thu muc assets
     * @param ambient  do sang moi truong (quai vat de toi hon nguoi choi mot chut)
     */
    public HumanoidFigure(String skinPath, float ambient) {
        this(skinPath, ambient, ambient, ambient);
    }

    /** Nhu tren nhung anh sang co mau - dung de ve con quai dang nhap nhay DO vi trung don. */
    public HumanoidFigure(String skinPath, float red, float green, float blue) {
        skin = new Texture(Gdx.files.internal(skinPath));
        skin.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        model = PlayerMesh.build(skin, U);
        instance = new ModelInstance(model);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, red, green, blue, 1f));

        for (int i = 0; i < LIMB_NAMES.length; i++) {
            Node limb = instance.getNode(LIMB_NAMES[i]);
            limbs[i] = limb;
            if (limb != null) {
                // BAT BUOC. Mac dinh libGDX coi localTransform la do no quan ly: moi lan goi
                // calculateTransforms() no dung lai localTransform tu translation/rotation/scale
                // (deu dang dung yen) - tuc XOA sach goc xoay minh vua dat, nen tay chan khong
                // bao gio nhuc nhich. Bat co nay len la bao "khuc nay TAO tu tinh, dung dong vao".
                limb.isAnimated = true;
            }
        }
    }

    /**
     * Dat hinh nguoi vao the gioi va vung tay chan theo nhip di.
     *
     * @param feet vi tri ban chan
     * @param yaw  huong mat quay ve (do)
     */
    public void pose(Vector3 feet, float yaw, WalkCycle cycle) {
        instance.transform.setToTranslation(feet).rotate(Vector3.Y, yaw);

        // Chan trai / chan phai vung NGUOC chieu nhau -> chan truoc chan sau nhu dang buoc.
        // Tay vung nguoc lai voi chan cung ben, cong them cu quo tay khi danh.
        float swing = cycle.legAngle();
        float punch = cycle.punchAngle();
        swingLimb(LEG_LEFT, swing, HIP_Y);
        swingLimb(LEG_RIGHT, -swing, HIP_Y);
        swingLimb(ARM_LEFT, -swing, SHOULDER_Y);
        swingLimb(ARM_RIGHT, swing - punch, SHOULDER_Y);
    }

    /**
     * Xoay mot tay/chan quanh khop noi voi than o cao do {@code pivotY}.
     *
     * Khuc thit duoc dung san o dung cho cua no trong luoi, nen phai keo khop ve goc toa do,
     * xoay, roi day trai lai - nhu vay chan xoay quanh HONG chu khong quanh ban chan.
     */
    private void swingLimb(int index, float degrees, float pivotY) {
        Node limb = limbs[index];
        if (limb == null) {
            return;
        }
        limb.localTransform.idt()
                .translate(0f, pivotY * U, 0f)
                .rotate(Vector3.X, degrees)
                .translate(0f, -pivotY * U, 0f);
        limb.calculateTransforms(true);
    }

    public void begin(PerspectiveCamera camera) {
        batch.begin(camera);
    }

    /** Ve hinh nguoi voi tu the vua dat o {@link #pose}. */
    public void draw() {
        batch.render(instance, environment);
    }

    public void end() {
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        model.dispose();
        skin.dispose();
    }
}
