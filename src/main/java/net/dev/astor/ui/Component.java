package net.dev.astor.ui;

import java.util.concurrent.atomic.AtomicInteger;

public interface Component {
    void draw(AtomicInteger offset);
    void update(int mousePosX, int mousePosY);

    boolean mouseDown(int x, int y, int button);

    void mouseReleased(int x, int y, int button);

    boolean keyTyped(char chatTyped, int keyCode);

    void setComponentStartAt(int newOffsetY);
    int getHeight();
    boolean isVisible();

    default boolean isBinding() {
        return false;
    }
}