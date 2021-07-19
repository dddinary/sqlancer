package riffle.lock;


public class LockInterval {
    private int start;
    private int end;

    public LockInterval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean include(int x) {
        return start <= x && x <= end;
    }

    public boolean cross(LockInterval other) {
        return !(other.start > end || other.end < start);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
