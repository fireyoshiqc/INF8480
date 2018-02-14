package ca.polymtl.inf8480.calculs.computeserver;

import ca.polymtl.inf8480.calculs.shared.OperationPair;

import java.util.ArrayList;

public class ComputeServer {

    public static void main(String args[]) {

    }

    public int run(ArrayList<OperationPair> ops) {
        int sum = 0;
        for (OperationPair op : ops)
        switch (op.operation.toLowerCase()) {
            case "pell":
                sum += Operations.pell(op.arg) % 4000;
                break;
            case "prime":
                sum += Operations.prime(op.arg) % 4000;
                break;
        }
        return sum;
    }

}
