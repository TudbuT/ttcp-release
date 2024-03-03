package de.tudbut.mod.client.ttcp.gui;

import de.tudbut.type.Vector3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.EventHandler;
import de.tudbut.mod.client.ttcp.mods.combat.AutoTotem;
import de.tudbut.mod.client.ttcp.mods.combat.HopperAura;
import de.tudbut.mod.client.ttcp.mods.combat.PopCount;
import de.tudbut.mod.client.ttcp.mods.rendering.PlayerSelector;
import de.tudbut.mod.client.ttcp.mods.rendering.HUD;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.obj.Vector2i;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class GuiTTCIngame extends Gui {
    
    public static void draw() {
        new GuiTTCIngame().drawImpl();
    }
    
    public static void drawOffhandSlot(int x, int y) {
        new GuiTTCIngame().drawOffhandSlot0(x,y);
    }
    
    public void drawOffhandSlot0(int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        TTCp.mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/widgets.png"));
        drawTexturedModalRect(x, y, 24, 22, 29, 24);
    }
    
    public static void drawItem(int x, int y, float partialTicks, EntityPlayer player, ItemStack stack) {
        Method m = Utils.getMethods(GuiIngame.class, int.class, int.class, float.class, EntityPlayer.class, ItemStack.class)[0];
        m.setAccessible(true);
        try {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderHelper.enableGUIStandardItemLighting();
            m.invoke(Minecraft.getMinecraft().ingameGUI, x,y,partialTicks,player,stack);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    public void drawImpl() {
        ScaledResolution sr = new ScaledResolution(TTCp.mc);
        Vector2i screenSize = new Vector2i(sr.getScaledWidth(), sr.getScaledHeight());
    
        int y = sr.getScaledHeight() - (5 + TTCp.mc.fontRenderer.FONT_HEIGHT);
        int x = screenSize.getX() - 5;
    
        if(!TTCp.isIngame())
            return;
        
        y = drawPos(TTCp.player, "Player", x, y);
        if(TTCp.mc.getRenderViewEntity() != TTCp.player)
            y = drawPos(Objects.requireNonNull(TTCp.mc.getRenderViewEntity()), "Camera", x, y);
    
        drawString("Ping: " + EventHandler.ping[0] + " | TPS: " + (Utils.roundTo(EventHandler.tps, 2)) + " | Players: " + EventHandler.ping[1] + "/" + EventHandler.ping[2], x, y, 0xff00ff00);
        y -= 10;
        
        y -= 10;
        
        for (int i = 0; i < TTCp.modules.length; i++) {
            Module module = TTCp.modules[i];

            if(module == null)
                return;
            if(module.enabled && module.displayOnClickGUI()) {
                int color = 0x000000;
                
                switch (module.danger()) {
                    case 0:
                        color = 0x00ff00;
                        break;
                    case 1:
                        color = 0x80ff00;
                        break;
                    case 2:
                        color = 0xffff00;
                        break;
                    case 3:
                        color = 0xff8000;
                        break;
                    case 4:
                        color = 0xff0000;
                        break;
                    case 5:
                        color = 0xff00ff;
                        break;
                }
                
                
                drawString(module.toString(), x, y, color);
                y-=10;
            }
        }
    
        /*if(AutoCrystal.getInstance().build) {
            String s = "AutoCrystal build enabled!";
            drawString(s, sr.getScaledWidth() / 2 + TTCp.mc.fontRenderer.getStringWidth(s) / 2, sr.getScaledHeight() / 2 - 20, 0xffff0000);
        }
        if(AutoCrystal.getInstance().enabled) {
            String s = "AutoCrystal state:";
            drawString(s, sr.getScaledWidth() / 2 + TTCp.mc.fontRenderer.getStringWidth(s) / 2, sr.getScaledHeight() / 2 + 10, 0xffff0000);
            s = AutoCrystal.getInstance().state.toString();
            drawString(s, sr.getScaledWidth() / 2 + TTCp.mc.fontRenderer.getStringWidth(s) / 2, sr.getScaledHeight() / 2 + 20, 0xffff0000);
            s = AutoCrystal.getInstance().crystalsPlaced.size() + " crystals on target, " + (Utils.getEntities(EntityEnderCrystal.class, entityEnderCrystal -> entityEnderCrystal.getDistance(TTCp.player) <= AutoCrystal.getInstance().crystalRange).length) + " in distance";
            drawString(s, sr.getScaledWidth() / 2 + TTCp.mc.fontRenderer.getStringWidth(s) / 2, sr.getScaledHeight() / 2 + 40, 0xffff0000);
        }*/
        if(HopperAura.getInstance().enabled) {
            String s = "HopperAura state:";
            drawString(s, sr.getScaledWidth() / 2 + TTCp.mc.fontRenderer.getStringWidth(s) / 2, sr.getScaledHeight() / 2 + 60, 0xffff0000);
            s = HopperAura.state.toString();
            drawString(s, sr.getScaledWidth() / 2 + TTCp.mc.fontRenderer.getStringWidth(s) / 2, sr.getScaledHeight() / 2 + 70, 0xffff0000);
        }
    
        Notifications notifications = TTCp.getModule(Notifications.class);
        if(notifications.enabled) {
            x = sr.getScaledWidth() / 2 - (300 / 2);
            y = sr.getScaledHeight() / 4;
            
            Notifications.Notification[] notifs = Notifications.getNotifications().toArray(new Notifications.Notification[0]);
            for (int i = 0; i < notifs.length; i++) {
                drawRect(x, y, x + 300, y + 30, 0x80202040);
                drawStringL(notifs[i].text, x + 10, y + (15 - (9 / 2)), 0xffffffff);
                y -= 35;
            }
        }
        
        if(TTCp.getModule(PlayerSelector.class).enabled) {
            try {
                PlayerSelector.render();
            } catch (Exception ignored) {

            }
        }
    
        AutoTotem autoTotem = TTCp.getModule(AutoTotem.class);
        if(HUD.getInstance().showPopPredict) {
            PopCount popCount = TTCp.getModule(PopCount.class);
            PopCount.Counter counter = popCount.counters.get(TTCp.player);
            if (counter != null && counter.isPopping()) {
                x = sr.getScaledWidth() / 2 - (200 / 2);
                y = sr.getScaledHeight() - sr.getScaledHeight() / 3;
                drawRect(x - 1, y - 1, x + 200 + 1, y + 20 + 1, 0x40202040);
                float f = counter.predictPopProgress();
                if (f >= 0.95)
                    drawRect(x, y, x + 200, y + 20, 0xffff0000);
                else
                    drawRect(x, y, (int) (x + (f * 200)), y + 20, 0x80000000 + (0xff << (int) Math.ceil((f * 16))));
                drawStringL((int) (f * 100) + "%", x + 6, y + 6, 0xffffffff);
            }
            else if (counter == null) {
                System.out.println("PopCount counter null?");
                ChatUtils.chatPrinterDebug().println("PopCount counter null? ");
            }
        }
    }
    
    private void drawString(String s, int x, int y, int color) {
        drawString(
                TTCp.mc.fontRenderer,
                s,
                x - TTCp.mc.fontRenderer.getStringWidth(s),
                y,
                color
        );
    }
    
    private void drawStringL(String s, int x, int y, int color) {
        drawString(
                TTCp.mc.fontRenderer,
                s,
                x,
                y,
                color
        );
    }
    
    private int drawPos(Entity e, String s, int x, int y) {
        Vector3d p = new Vector3d(e.posX, e.posY, e.posZ);
        
        p.setX(Math.round(p.getX() * 10d) / 10d);
        p.setY(Math.round(p.getY() * 10d) / 10d);
        p.setZ(Math.round(p.getZ() * 10d) / 10d);
    
        if(TTCp.mc.world.provider.getDimension() == -1)
            drawString(
                    s + " Overworld " + Math.round(p.getX() * 8 * 10d) / 10d + " " + Math.round(p.getY() * 10d) / 10d + " " + Math.round(p.getZ() * 8 * 10d) / 10d,
                    x, y, 0xff00ff00
            );
        if(TTCp.mc.world.provider.getDimension() == 0)
            drawString(
                    s + " Nether " + Math.round(p.getX() / 8 * 10d) / 10d + " " + Math.round(p.getY() * 10d) / 10d + " " + Math.round(p.getZ() / 8 * 10d) / 10d,
                    x, y, 0xff00ff00
            );
        y -= 10;
        drawString(s + " " + p.getX() + " " + p.getY() + " " + p.getZ(), x, y, 0xff00ff00);
        return y - 10;
    }
}
