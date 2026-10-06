package com.arkit.scavvillager;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 绑定到实体的语音（原版 BoundVoiceSound 的移植）。
 * 每 tick 从实体身上取坐标刷新音源位置，所以声音会跟着村民/掠夺者移动——这是原 Forge 模组的招牌效果，
 * 也是资源包做不到的部分。
 */
public class VoiceSound extends AbstractTickableSoundInstance {
    /** 安全上限：语音都很短，超过这个时间还在播就强制收掉，避免实例泄漏。 */
    private static final int MAX_LIFETIME_TICKS = 20 * 20;

    private final Entity entity;
    private final int kind;
    private final int priority;
    private final boolean keepAfterDeath;

    /** 实体消失后停止跟随、停在原地把最后一句播完。 */
    private boolean detached;
    private int age;

    public VoiceSound(Entity entity, SoundEvent event, int kind) {
        super(event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        ScavConfig cfg = ScavConfig.get();
        this.entity = entity;
        this.kind = kind;
        this.priority = VoiceKind.priority(kind);
        this.keepAfterDeath = VoiceKind.keepAfterDeath(kind);
        this.volume = (float) cfg.volume;
        this.pitch = babyPitchOf(entity, cfg);
        this.attenuation = Attenuation.LINEAR;
        syncPosition();
    }

    private void syncPosition() {
        this.x = this.entity.getX();
        this.y = this.entity.getY() + this.entity.getBbHeight() * 0.85;
        this.z = this.entity.getZ();
    }

    @Override
    public void tick() {
        this.age++;
        if (this.age > MAX_LIFETIME_TICKS) {
            finish();
            return;
        }
        if (this.entity.isRemoved() || !this.entity.isAlive()) {
            if (this.keepAfterDeath && !this.detached) {
                this.detached = true;      // 留在死亡地点播完
                return;
            }
            if (!this.keepAfterDeath) {
                finish();
            }
            return;
        }
        syncPosition();
    }

    /** 结束这条语音（被更高优先级打断、或生物消失时调用）。父类的 stop() 是 final，不能覆写。 */
    public void finish() {
        this.detached = true;
        super.stop();
    }

    public int kind() {
        return this.kind;
    }

    public int priority() {
        return this.priority;
    }

    public Entity entity() {
        return this.entity;
    }

    /** 只有启用了小村民变音时才抬高音高（原版配置项 babyPitch）。 */
    static float babyPitchOf(Entity entity, ScavConfig cfg) {
        if (!cfg.babyPitch || !(entity instanceof LivingEntity living)) {
            return 1.0F;
        }
        return living.isBaby() ? 1.5F : 1.0F;
    }
}
