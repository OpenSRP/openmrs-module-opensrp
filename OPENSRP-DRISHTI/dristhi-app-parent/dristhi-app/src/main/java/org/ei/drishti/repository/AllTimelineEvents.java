package org.ei.drishti.repository;

import java.util.List;

import org.ei.drishti.domain.TimelineEvent;

public class AllTimelineEvents {
    private TimelineEventRepository repository;

    public AllTimelineEvents(TimelineEventRepository repository) {
        this.repository = repository;
    }

    public List<TimelineEvent> forCase(String caseId) {
        return repository.allFor(caseId);
    }

    public void add(TimelineEvent timelineEvent) {
        repository.add(timelineEvent);
    }
}
