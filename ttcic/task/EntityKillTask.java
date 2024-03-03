package tudbut.mod.client.ttcp.ttcic.task;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import tudbut.mod.client.ttcp.TTCp;
import tudbut.mod.client.ttcp.ttcic.Task;
import tudbut.mod.client.ttcp.ttcic.Account;
import tudbut.obj.Transient;
import tudbut.mod.client.ttcp.mods.combat.KillAura;

import static tudbut.mod.client.ttcp.ttcic.TTCIC.ttcic;

public class EntityKillTask extends Task {

    @Transient
    KillAura aura = TTCp.getModule(KillAura.class);
    @Transient
    boolean oldEnabled = false;
    @Transient
    ArrayList<String> oldTargets = new ArrayList<>();

    Account toAttack = null;

    public EntityKillTask(Account toKill) {
        ttcic.write("Please provide a list of players in your RD.");
        ttcic.writeAccounts();
        toAttack = ttcic.localize(toAttack);
    }

    @Override
    protected void onTick() {
        if (toAttack.location.get().distanceTo(mc.player.getPositionVector()) > 4) {
            ttcic.taskQueue.startNow(new EntityFollowTask(toAttack));
            return;
        }
        EntityPlayer player = mc.world.getPlayerEntityByUUID(toAttack.uuid);
        if (player == null || player.getHealth() == 0) {
            done();
            return;
        }
    }

    @Override
    public void unpauseOrStart() {
        this.oldEnabled = aura.enabled;
        this.oldTargets = aura.targets;

        if (!aura.enabled)
            aura.toggle();
        aura.targets = new ArrayList<>();
        aura.targets.add(toAttack.name);
    }

    @Override
    public void pauseOrStop() {
        if (this.oldEnabled != aura.enabled)
            aura.toggle();
        aura.targets = this.oldTargets;
    }
}