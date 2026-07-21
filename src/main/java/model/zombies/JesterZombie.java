package model.zombies;

import model.GameSession;
import model.Projectile;
import model.enums.ProjectileType;
import model.enums.ZombieType;

import java.util.ArrayList;
import java.util.List;

/**
 * زامبی ژانگولر — وقتی تیری بیاید می‌چرخد و تیرها را برمی‌گرداند.
 */
public class JesterZombie extends Zombie {

    private boolean spinning;
    private List<Projectile> deflectedProjectiles;

    public JesterZombie() {
        super(ZombieType.JESTER_ZOMBIE, 490, 100, 0.12, 450);
        this.spinning = false;
        this.deflectedProjectiles = new ArrayList<>();
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        if (spinning && deflectedProjectiles.isEmpty()) {
            spinning = false;
        }
    }

    /** آغاز چرخش هنگام برخورد تیر */
    public void startSpinning() {
        spinning = true;
        moveSpeed = 0.25;
    }

    /** توقف چرخش */
    public void stopSpinning() {
        spinning = false;
        moveSpeed = 0.12;
    }

    /** تیر را دریافت و برمی‌گرداند */
    public Projectile deflect(Projectile incoming) {
        if (!spinning) {
            startSpinning();
        }
        Projectile deflected = new Projectile(
            incoming.getType(), x, y, incoming.getDamage());
        deflected.setMovingRight(false);
        deflected.setHitsPlants(true);
        if (incoming.getType() == ProjectileType.ICE) {
            deflected.setMovingRight(false);
        }
        return deflected;
    }

    public boolean isSpinning() {
        return spinning;
    }

    @Override
    public String getDescription() {
        return "Jester Zombie: Deflects all projectiles back at plants "
               + "while spinning! Ice projectiles freeze plants.";
    }
}
