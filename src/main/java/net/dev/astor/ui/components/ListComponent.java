package net.dev.astor.ui.components;

import net.dev.astor.property.Property;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ColorProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ListProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.dev.astor.property.properties.TextProperty;
import net.dev.astor.ui.Component;
import net.dev.astor.ui.dataset.impl.FloatSlider;
import net.dev.astor.ui.dataset.impl.IntSlider;
import net.dev.astor.ui.dataset.impl.PercentageSlider;
import net.dev.astor.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Header row for a {@link ListProperty}: the property name plus a solid gold triangle that points up
 * when collapsed and flips upside down when expanded. Expanding reveals the group's child components
 * underneath, and the row carries nothing else - no ":" and no value text.
 */
public class ListComponent implements Component {
    private static final int ROW_HEIGHT = 12;
    private static final int TRIANGLE_SIZE = 4;

    /**
     * How far the children are shifted right of the header when the group is open, so a nested
     * property reads as belonging to the group rather than sitting beside it on the same margin.
     *
     * <p>The panel is 92 wide and a slider already reaches from x+4 to x+88, so four pixels is all
     * that fits: x+8 to x+92, and the category background is drawn out to x+94.</p>
     */
    private static final int CHILD_INDENT = 4;

    /**
     * The tree that ties the open group's children back to its header.
     *
     * <p>{@link #TREE_X} puts the trunk directly under the header's own name rather than out at the
     * panel margin, so the line reads as growing out of the group instead of running beside it.
     * {@link #TREE_TOP} is where it starts: the name is drawn at row + 5 through a half-scaled 9px
     * font, which reaches about row + 9.5, so the line begins below the glyphs instead of through
     * them. {@link #TREE_BRANCH} is how far each branch runs right from the trunk, and it stops a
     * pixel short of where the child's content begins rather than touching it.</p>
     *
     * <p>Three pixels is all a branch can be. The children are indented to {@link #CHILD_INDENT},
     * the trunk sits at {@link #TREE_X} and takes one of those pixels, and the panel has no more
     * margin to give - a longer branch means pulling the children in, which means the nested
     * sliders stop being full width.</p>
     */
    private static final int TREE_X = 4;
    private static final int TREE_TOP = 10;
    private static final int TREE_BRANCH = 3;

    /** Both the trunk and the branches are one pixel wide, drawn as rects rather than GL lines. */
    private static final int LINE = 1;

    private static final Color GOLD = ColorUtil.GOLD;

    private final ListProperty property;
    private final ModuleComponent module;
    private final List<Component> children = new ArrayList<>();
    private boolean expanded;
    private int offsetY;
    private int x;
    private int y;

    public ListComponent(ListProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
        for (Property<?> child : property.getChildren()) {
            Component c = this.createChild(child);
            if (c != null) {
                this.children.add(c);
            }
        }
        this.layoutChildren();
    }

    private Component createChild(Property<?> child) {
        // Mirrors ModuleComponent's dispatch so every scalar type keeps working when nested.
        if (child instanceof BooleanProperty) {
            return new CheckBoxComponent((BooleanProperty) child, this.module, 0);
        } else if (child instanceof FloatProperty) {
            return new SliderComponent(new FloatSlider((FloatProperty) child), this.module, 0);
        } else if (child instanceof IntProperty) {
            return new SliderComponent(new IntSlider((IntProperty) child), this.module, 0);
        } else if (child instanceof PercentProperty) {
            return new SliderComponent(new PercentageSlider((PercentProperty) child), this.module, 0);
        } else if (child instanceof ModeProperty) {
            return new ModeComponent((ModeProperty) child, this.module, 0);
        } else if (child instanceof ColorProperty) {
            return new ColorPickerComponent((ColorProperty) child, this.module, 0);
        } else if (child instanceof TextProperty) {
            return new TextComponent((TextProperty) child, this.module, 0);
        }
        return null;
    }

    /**
     * CategoryComponent.layout() calls setComponentStartAt() then getHeight() on every draw, so the
     * child offsets have to be assigned here rather than in update() - update() only runs while the
     * mouse is over the GUI (ClickGui:106), which would leave children misplaced otherwise.
     *
     * <p>A hidden child still occupies no row. Advancing past one anyway is what used to leave a
     * gap here while getHeight() counted it as nothing, so every row below it was drawn lower than
     * the row the click was tested against.</p>
     */
    private void layoutChildren() {
        int childY = this.offsetY + ROW_HEIGHT;
        for (Component c : this.children) {
            c.setComponentStartAt(childY);
            if (c.isVisible()) {
                childY += c.getHeight();
            }
        }
    }

    public void draw(AtomicInteger offset) {
        int baseX = this.module.category.getX() + 4;
        int rowY = this.module.category.getY() + this.offsetY;

        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft.getMinecraft()
                .fontRendererObj
                .drawString(this.property.getName().replace("-", " "), (float) (baseX * 2), (float) ((rowY + 5) * 2), -1, false);
        GL11.glPopMatrix();

        this.drawTriangle(rowY);
        this.drawTree(rowY);

        if (this.expanded) {
            // The children all lay themselves out from category.getX() and their own offsetY, with
            // no indent of their own, so the indent is applied here instead. Drawing inside a
            // translated matrix and testing the mouse against the same translated coordinates
            // (see update/mouseDown) is what keeps the hit boxes on the pixels they are drawn on.
            // try/finally because a matrix left on the stack misplaces every later component.
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(CHILD_INDENT, 0.0F, 0.0F);
                for (Component c : this.children) {
                    if (c.isVisible()) {
                        c.draw(offset);
                        offset.incrementAndGet();
                    }
                }
            } finally {
                GL11.glPopMatrix();
            }
        }
    }

    /**
     * Draws the bracket that connects an open group's children back to its header: one vertical
     * trunk running from the middle of the header row down to the last child, and a short horizontal
     * branch onto the middle of every child row.
     *
     * <p>The rows are walked exactly the way {@link #layoutChildren()} places them - same starting
     * offset, same "only visible children take up space" rule - so a branch can never drift away
     * from the row it belongs to when a child's visibility changes.</p>
     *
     * <p>Stops with the trunk at the last child rather than running on past it, so the line ends
     * with the group instead of trailing down the panel.</p>
     *
     * <p>Drawn in panel space rather than inside the indented child transform, which is why this
     * runs before that transform is pushed: the trunk has to sit to the left of the children, which
     * is outside the region the indent covers.</p>
     */
    private void drawTree(int headerRowY) {
        if (!this.expanded) {
            return;
        }
        List<Integer> rowCenters = new ArrayList<>();
        int childY = this.offsetY + ROW_HEIGHT;
        for (Component c : this.children) {
            if (c.isVisible()) {
                rowCenters.add(this.module.category.getY() + childY + c.getHeight() / 2);
                childY += c.getHeight();
            }
        }
        if (rowCenters.isEmpty()) {
            return;
        }
        int trunkX = this.module.category.getX() + TREE_X;
        int gold = GOLD.getRGB();

        int last = rowCenters.get(rowCenters.size() - 1);
        Gui.drawRect(trunkX, headerRowY + TREE_TOP, trunkX + LINE, last + LINE, gold);
        for (int center : rowCenters) {
            Gui.drawRect(trunkX, center, trunkX + TREE_BRANCH, center + LINE, gold);
        }
    }

    private void drawTriangle(int rowY) {
        int right = this.module.category.getX() + this.module.category.getWidth() - 8;
        int top = rowY + 3;
        int left = right - TRIANGLE_SIZE;
        int middle = left + TRIANGLE_SIZE / 2;
        int bottom = top + TRIANGLE_SIZE;

        GL11.glPushMatrix();
        // Cull face and depth off, the same pair RenderUtil.enableRenderState() asks for. The two
        // orientations of this shape wind in opposite directions - the collapsed one is (bottom,
        // bottom, apex) and the expanded one (top, top, apex) - so with cull face on, whichever one
        // ends up back-facing is thrown away and the triangle vanishes the moment the group opens.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(GOLD.getRed() / 255.0F, GOLD.getGreen() / 255.0F, GOLD.getBlue() / 255.0F, 1.0F);

        GL11.glBegin(GL11.GL_TRIANGLES);
        if (this.expanded) {
            // Pointing down.
            GL11.glVertex2i(left, top);
            GL11.glVertex2i(left + TRIANGLE_SIZE, top);
            GL11.glVertex2i(middle, bottom);
        } else {
            // Pointing up.
            GL11.glVertex2i(left, bottom);
            GL11.glVertex2i(left + TRIANGLE_SIZE, bottom);
            GL11.glVertex2i(middle, top);
        }
        GL11.glEnd();

        // resetColor() rather than a raw glColor4f back to white: GlStateManager caches the current
        // colour, so setting it directly leaves that cache stale and the next Gui.drawRect asking for
        // the colour it believes is already set skips the call entirely.
        GlStateManager.resetColor();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    @Override
    public int getHeight() {
        int h = ROW_HEIGHT;
        if (this.expanded) {
            for (Component c : this.children) {
                if (c.isVisible()) {
                    h += c.getHeight();
                }
            }
        }
        return h;
    }

    public void update(int mousePosX, int mousePosY) {
        this.x = this.module.category.getX();
        this.y = this.module.category.getY() + this.offsetY;
        for (Component c : this.children) {
            c.update(mousePosX - CHILD_INDENT, mousePosY);
        }
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        this.layoutChildren();
    }

    /**
     * Children first, then the header.
     *
     * <p>The children were never offered a click before: ModuleComponent hands every click inside
     * its rows to each setting in turn, and this class answered only about its own header row. So an
     * open group's checkbox would not toggle and its slider would not move, however correct their
     * own hit tests were.</p>
     *
     * <p>They are tested before the header rather than the other way round because a child's row
     * starts one row below this one and the two never overlap, but a child that returns true for a
     * click it does not own would otherwise collapse the group underneath it.</p>
     */
    public boolean mouseDown(int x, int y, int button) {
        if (!this.module.panelExpand) {
            return false;
        }
        if (this.expanded) {
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component c = this.children.get(i);
                if (c.isVisible() && c.mouseDown(x - CHILD_INDENT, y, button)) {
                    return true;
                }
            }
        }
        if (button == 0 && this.isHovered(x, y)) {
            this.expanded = !this.expanded;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        // SliderComponent only clears its dragging flag here, so without this a slider left inside an
        // open group kept following the cursor after the button came back up.
        for (Component c : this.children) {
            if (c.isVisible()) {
                c.mouseReleased(x - CHILD_INDENT, y, button);
            }
        }
    }

    @Override
    public boolean keyTyped(char chatTyped, int keyCode) {
        if (!this.expanded) {
            return false;
        }
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Component c = this.children.get(i);
            if (c.isVisible() && c.keyTyped(chatTyped, keyCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + ROW_HEIGHT - 1;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}
