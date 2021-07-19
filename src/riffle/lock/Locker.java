package riffle.lock;

import java.util.*;

public class Locker {
    private final Map<Integer, Set<Lock>> lockMap = new HashMap<>();

    public Locker(int n) {
        for (int id = 1; id <= n; id++) {
            lockMap.put(id, new HashSet<>());
        }
    }

    public Locker(List<Integer> idList) {
        for (int id : idList) {
            lockMap.put(id, new HashSet<>());
        }
    }

    public boolean couldRun(int sid, Set<Lock> wantedLocks) {
        boolean ok = true;
        outer:
        for (int i : lockMap.keySet()) {
            if (i == sid) continue;
            for (Lock exitLock : lockMap.get(i)) {
                for (Lock lock : wantedLocks) {
                    if (!exitLock.compatibleWith(lock)) {
                        ok = false;
                        break outer;
                    }
                }
            }
        }
        if (ok) {
            lockMap.get(sid).addAll(wantedLocks);
        }
        return ok;
    }


}
