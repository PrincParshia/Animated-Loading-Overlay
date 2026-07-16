package princ.animatedloadingoverlay.client;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    public static final String NAMESPACE = "animated_loading_overlay";
    public static final String NAME = "Animated Loading Overlay";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    public static final Identifier SOUND = withDefaultNamespace("sounds/ui/loading_overlay.wav");

    public static final int FPS = 30;
    public static final int FRAMES = 129;

    public static final int FRAME_WIDTH = 1280;
    public static final int FRAME_HEIGHT = 720;
    public static final int COLS = 4;
    public static final int ROWS = 8;
    public static final int FRAMES_PER_SHEET = COLS * ROWS;
    public static final int SHEET_COUNT = 5;
    public static final int SHEET_WIDTH = COLS * FRAME_WIDTH;
    public static final int SHEET_HEIGHT = ROWS * FRAME_HEIGHT;

    public static final float SCALE = 1280f / 1920f;

    public static final Identifier BACKGROUND = withDefaultNamespace("textures/gui/title/0.png");
    public static final Identifier[] SHEETS = new Identifier[SHEET_COUNT];

    static {
        for (int i = 0; i < SHEET_COUNT; i++) {
            SHEETS[i] = withDefaultNamespace("textures/gui/title/" + (i + 1) + ".png");
        }
    }

    public static Identifier withDefaultNamespace(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }
}