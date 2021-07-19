package riffle.lock;


import java.util.HashSet;
import java.util.Set;

public class Lock {
    enum LockType {READ_LOCK, WRITE_LOCK}

    private LockType type;
    private Set<Integer> lockPoints = new HashSet<>();
    private Set<LockInterval> lockIntervals = new HashSet<>();

    private Lock(LockType type) {
        this.type = type;
    }

    public static class LockBuilder {
        Lock lock;
        public LockBuilder(LockType type) {
            lock = new Lock(type);
        }

        public void lock(int point) {
            lock.lockPoints.add(point);
        }

        public void lock(int start, int end) {
            lock.lockIntervals.add(new LockInterval(start, end));
        }

        public Lock build() {
            return lock;
        }
    }

    public boolean compatibleWith(Lock other) {
        if (this.type == LockType.READ_LOCK && other.type == LockType.READ_LOCK) {
            return true;
        }
        for (int x : other.getLockPoints()) {
            if (lockPoints.contains(x)) return false;
            for (LockInterval interval: lockIntervals) {
                if (interval.include(x)) return false;
            }
        }
        for (LockInterval interval : other.getLockIntervals()) {
            for (int x : lockPoints) {
                if (interval.include(x)) return false;
            }
            for (LockInterval itv : lockIntervals) {
                if (itv.cross(interval)) return false;
            }
        }
        return true;
    }

    public LockType getType() {
        return type;
    }

    public void setType(LockType type) {
        this.type = type;
    }

    public Set<Integer> getLockPoints() {
        return lockPoints;
    }

    public void setLockPoints(Set<Integer> lockPoints) {
        this.lockPoints = lockPoints;
    }

    public Set<LockInterval> getLockIntervals() {
        return lockIntervals;
    }

    public void setLockIntervals(Set<LockInterval> lockIntervals) {
        this.lockIntervals = lockIntervals;
    }
}
