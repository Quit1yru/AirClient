package net.ccbluex.liquidbounce.utils.client;

import net.minecraft.util.MouseHelper;

class RawMouseHelper extends MouseHelper {
    @Override
    public void mouseXYChange()
    {
        deltaX = RawInputMod.dx;
        RawInputMod.dx = 0;
        deltaY = -RawInputMod.dy;
        RawInputMod.dy = 0;
    }
}


