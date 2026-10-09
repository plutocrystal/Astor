package net.dev.astor.ui;

import net.dev.astor.module.Module;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class ToolTip {
    
    private static final long DELAY_MS = 3000L;
    
    private static final int GAP = 8;
    
    private static final int COLOR = 0xFFFFFFFF;
    private static final int SHADOW_COLOR = 0xFF101010;
    private static final int LINE_HEIGHT = 10;
    
    private static final int MIN_LINE_WIDTH = 60;

    private Module hovered;
    private long hoverStart;

    public void update(Module hovered, long now) {
        if (hovered != this.hovered) {
            this.hovered = hovered;
            this.hoverStart = now;
        }
    }

    public boolean isVisible(long now) {
        return this.hovered != null && !this.hovered.getDescription().isEmpty() && now - this.hoverStart >= DELAY_MS;
    }

    public void reset() {
        this.hovered = null;
        this.hoverStart = 0L;
    }

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
        
        for (String line : lines) {
            if (y > screenHeight - LINE_HEIGHT) {
                break;
            }
            font.drawString(line, x + 1.0F, y + 1.0F, SHADOW_COLOR, false);
            font.drawString(line, (float) x, (float) y, COLOR, false);
            y += LINE_HEIGHT;
        }
    }

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