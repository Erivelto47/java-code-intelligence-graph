package com.example.decisiontrace.ifreturn;

public class ImportService {
    public boolean process(ImportRequest request) {
        if (request.processed()) {
            return false;
        }

        return true;
    }

    public record ImportRequest(boolean processed) {
    }
}
