public class TestInput {
    public int compute(int value) {
        int result = value * 2;

        if (result > 10) {
            result = result - 1;
        } else {
            result = result + 1;
        }

        return result;
    }

    public int countdown(int start) {
        int current = start;

        while (current > 0) {
            current = current - 1;
        }

        return current;
    }
}
