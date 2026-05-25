package com.example.decisiontrace.earlyreturn;

public class ImportService {
    public ProcessingResult process(ImportRequest request) {
        if (request.processed()) {
            return ProcessingResult.alreadyProcessed(request.id());
        }

        return ProcessingResult.accepted(request.id());
    }

    public record ImportRequest(String id, boolean processed) {
    }

    public record ProcessingResult(String id, String status) {
        public static ProcessingResult alreadyProcessed(String id) {
            return new ProcessingResult(id, "ALREADY_PROCESSED");
        }

        public static ProcessingResult accepted(String id) {
            return new ProcessingResult(id, "ACCEPTED");
        }
    }
}
