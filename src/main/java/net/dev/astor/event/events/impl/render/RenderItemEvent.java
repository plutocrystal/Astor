package net.dev.astor.event.events.impl.render;

import net.dev.astor.event.events.callables.EventCancellable;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

/**
 * Fired at the head of the first-person hand render, before vanilla has transformed anything.
 *
 * <p>Cancelling skips vanilla's own transform entirely, which is how Animations substitutes its own
 * animation matrix. The values are a snapshot of what vanilla would have used, so a handler can reproduce
 * any of the vanilla branches unchanged - see {@link #getEnumAction()} and {@link #isUseItem()}.</p>
 */
public class RenderItemEvent extends EventCancellable {
    private final EnumAction enumAction;
    private final boolean useItem;
    private final float animationProgression;
    private final float partialTicks;
    private final float swingProgress;
    private final ItemStack itemToRender;

    public RenderItemEvent(EnumAction enumAction, boolean useItem, float animationProgression,
                           float partialTicks, float swingProgress, ItemStack itemToRender) {
        this.enumAction = enumAction;
        this.useItem = useItem;
        this.animationProgression = animationProgression;
        this.partialTicks = partialTicks;
        this.swingProgress = swingProgress;
        this.itemToRender = itemToRender;
    }

    /** The action vanilla would switch on. */
    public EnumAction getEnumAction() {
        return this.enumAction;
    }

    public boolean isUseItem() {
        return this.useItem;
    }

    /** Equip progress: 1 minus the interpolated equipped progress. */
    public float getAnimationProgression() {
        return this.animationProgression;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public float getSwingProgress() {
        return this.swingProgress;
    }

    public ItemStack getItemToRender() {
        return this.itemToRender;
    }
}
