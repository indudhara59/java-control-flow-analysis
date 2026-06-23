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
            if (current == 2) {
                break;
            }
            current = current - 1;
        }

        return current;
    }

    public int sumUntilNegative(int[] numbers) {
        int total = 0;

        for (int number : numbers) {
            if (number < 0) {
                continue;
            }
            total = total + number;
        }

        return total;
    }

    public String describe(int code) {
        switch (code) {
            case 1:
                return "one";
            case 2:
                return "two";
            default:
                return "many";
        }
    }

    public int guardedDivide(int left, int right) {
        try {
            return left / right;
        } catch (ArithmeticException exception) {
            return 0;
        } finally {
            System.out.println("done");
        }
    }
}
