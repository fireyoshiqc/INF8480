package ca.polymtl.inf8480.calculs.shared;

import java.io.Serializable;

public class OperationPair implements Serializable {
    public String operation;
    public int arg;

    public OperationPair(String operation, int arg) {
        this.operation = operation;
        this.arg = arg;
    }
}
