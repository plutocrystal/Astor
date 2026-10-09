package net.dev.astor.ui;

import net.dev.astor.module.Module;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * The hover note shown next to the cursor in the click gui.
 *
 * <p>Deliberately slow to appear: the cursor has to stay on one module for {@link #DELAY_MS} before
 * anything shows, and moving away resets it. Sweeping the cursor across the module list on the way to
 * somewhere else would otherwise trail a wall of text behind the pointer.</p>
 *
 * <p>The tooltip is bare text - no panel behind it - so the description is wrapped to the space left
 * on screen instead of being measured against a box, and a drop shadow carries it over whatever the
 * gui happens to be drawn on top of.</p>
 */
public class ToolTip {
    /** How long the cursor must rest on one module before the note appears. */
    private static final long DELAY_MS = 3000L;
    /** Gap between the cursor and the note. */
    private static final int GAP = 8;
    /** Opaque white. */
    private static final int COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF101010;
    private static final int LINE_HEIGHT = 10;
    /** Below this a description becomes one word per line, which is more noise than help. */
    private static final int MIN_LINE_WIDTH = 60;

    private Module hovered;
    private long hoverStart;

    /**
     * Records what the cursor is on and restarts the countdown unless it is still on the same module
     * as last frame.
     *
     * @param hovered the module under the cursor, or null when it is over nothing
     * @param now     the current time in milliseconds
     */
    public void update(Module hovered, long now) {
        if (hovered != this.hovered) {
            this.hovered = hovered;
            this.hoverStart = now;
        }
    }

    /** Whether the cursor has rested long enough on the current module. */
    public boolean isVisible(long now) {
        return this.hovered != null && !this.hovered.getDescription().isEmpty() && now - this.hoverStart >= DELAY_MS;
    }

    /** Forgets the current module, so reopening the gui does not resume a countdown that ran while it was closed. */
    public void reset() {
        this.hovered = null;
        this.hoverStart = 0L;
    }

    /**
     * Draws the note to the right of the cursor, wrapped to whatever screen space is left.
     *
     * <p>Called last in the gui frame so it lands on top of everything else. The note is moved left as
     * a whole rather than flipped to the cursor's other side, which keeps it beside the cursor for as
     * long as there is room for it.</p>
     */
    public void draw(FontRenderer font, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (this.hovered == null) {
            return;
        }
        List<String> lines = wrap(font, this.hovered.getDescription(), Math.max(MIN_LINE_WIDTH, screenWidth - mouseX - GAP));
        if (lines.isEmpty()) {
            return;
        }
        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, font.getStringWidth(line));
        }
        int x = mouseX + GAP;
        if (x + widest > screenWidth - GAP) {
            x = Math.max(GAP, screenWidth - GAP - widest);
        }
        int y = mouseY + GAP;
        // Nudged up rather than clamped to the bottom, so a long note starts next to the cursor and
        // runs off the lower edge instead of jumping away from where the user is looking.
        for (String line : lines) {
            if (y > screenHeight - LINE_HEIGHT) {
                break;
            }
            font.drawString(line, x + 1.0F, y + 1.0F, SHADOW_COLOR, false);
            font.drawString(line, (float) x, (float) y, COLOR, false);
            y += LINE_HEIGHT;
        }
    }

    /**
     * Breaks a description into lines no wider than {@code maxWidth}.
     *
     * <p>Split on spaces rather than characters so words stay readable, and a single word wider than
     * the limit is broken by character so it cannot push the line off screen.</p>
     */
    private static List<String> wrap(FontRenderer font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (font.getStringWidth(word) > maxWidth) {
                for (String chunk : breakUp(font, word, maxWidth)) {
                    lines.add(chunk);
                }
            } else {
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static List<String> breakUp(FontRenderer font, String word, int maxWidth) {
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            // Measured before the character goes on. Checking afterwards instead lets the chunk that
            // crossed the limit stay on the line, so it ends up wider than maxWidth.
            if (chunk.length() > 0 && font.getStringWidth(chunk.toString() + letter) > maxWidth) {
                chunks.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(letter);
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }
}