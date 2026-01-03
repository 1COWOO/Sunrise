package io.github._1cowoo.sunrisemod;

import java.util.Date;

/**
 * @author Nemo
 */
public class TimeAdapter {
    private final Date from;
    private final Date to;
    private final int tickOffset;
    private final int tickDuration;

    public TimeAdapter(Date from, Date to, Type type) {
        this.from = from;
        this.to = to;
        this.tickOffset = type.offset;
        this.tickDuration = type.period;
    }

    public boolean isValid() {
        long time = System.currentTimeMillis();
        return from.getTime() <= time && time < to.getTime();
    }

    public long getCurrentTick() {
        long fromTime = this.from.getTime();
        long period = to.getTime() - fromTime;
        long time = System.currentTimeMillis();
        long current = time - fromTime;
        long tick = tickDuration * current / period;
        return (tickOffset + tick) % 24000; // 24000틱 기준으로 순환
    }

    public enum Type {
        DAY(22835, 14315),
        NIGHT(37150, 9685);

        final int offset;
        final int period;

        Type(int offset, int period) {
            this.offset = offset;
            this.period = period;
        }
    }

    public Date getFrom() { return from; }
    public Date getTo() { return to; }
}
